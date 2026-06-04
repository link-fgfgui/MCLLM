package io.github.linkfgfgui.mcllm.markdownRenderer;

import com.bawnorton.mcchatgpt.config.Config;
import net.minecraft.network.chat.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//rewrite by claude
/**
 * Renders a markdown string into a Minecraft {@link Component} chain.
 *
 * <p>Parsing order (highest to lowest priority):
 * <ol>
 *   <li>Fenced code blocks  {@code ```…```}</li>
 *   <li>{@code <think>…</think>} tags</li>
 *   <li>Per-line markdown (headings, bold, italic, …)</li>
 *   <li>Inline code {@code `…`}</li>
 * </ol>
 */
public class MarkdownRenderer {

    // ── Compiled patterns ────────────────────────────────────────────────────

    /** Matches fenced code blocks (``` … ```) across multiple lines. */
    private static final Pattern BLOCK_CODE   = Pattern.compile("(?s)```(.*?)```");

    /** Matches inline code (`…`). */
    private static final Pattern INLINE_CODE  = Pattern.compile("`([^`]+)`");

    /** Matches {@code <think>…</think>} tags, stripping surrounding whitespace. */
    private static final Pattern THINK_TAG    = Pattern.compile("(?s)<think>\\s*(.*?)\\s*</think>");

    // Individual per-line patterns – easier to maintain than one giant alternation.
    // Note: no ^/$  / MULTILINE needed here because parseLine() receives a single
    // pre-split line and we match it with Matcher.matches() (implicit full-string match).
    private static final Pattern H3           = Pattern.compile("### (.+)");
    private static final Pattern H2           = Pattern.compile("## (.+)");
    private static final Pattern H1           = Pattern.compile("# (.+)");
    private static final Pattern BOLD         = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC       = Pattern.compile("\\*(.+?)\\*");
    private static final Pattern UNDERLINE    = Pattern.compile("__(.+?)__");
    private static final Pattern STRIKE       = Pattern.compile("~~(.+?)~~");
    private static final Pattern LINK         = Pattern.compile("\\[(.+?)]\\((.+?)\\)");

    // ── Colour constants ─────────────────────────────────────────────────────

    private static final int COLOR_H1        = 0x00AAFF;
    private static final int COLOR_H2        = 0x55AAFF;
    private static final int COLOR_H3        = 0xAAAAFF;
    private static final int COLOR_CODE      = 0xAAAAAA;
    private static final int COLOR_LINK      = 0x55AAFF;

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Converts a markdown string into a single Minecraft chat {@link Component}.
     *
     * @param markdown          raw markdown text (may contain fenced code blocks / think tags)
     * @param usageIsPresent    whether token-usage data is available
     * @param tokenUsed         number of tokens consumed
     * @param cost              monetary cost of the response
     * @param reasoning_content explicit reasoning content (used when {@code isThinkModel} is set)
     */
    public static Component render(
            String markdown,
            boolean usageIsPresent,
            long tokenUsed,
            float cost,
            String reasoning_content
    ) {
        Config config = Config.getInstance();

        // ── 1. Build the component list ──────────────────────────────────────
        List<Component> parts = new ArrayList<>();

        // Prefix label
        parts.add(Component.literal("<LLM> "));

        // Optional think/reasoning block (explicit parameter).
        // Guard against null: some model responses omit the reasoning_content field entirely.
        if (config.isThinkModel && reasoning_content != null && !reasoning_content.isEmpty()) {
            parts.add(buildThinkComponent(reasoning_content));
        }

        // Main content: split on fenced code blocks first
        parts.addAll(parseTopLevel(markdown, config));

        // Strip a trailing bare newline (cosmetic)
        if (!parts.isEmpty()) {
            Component last = parts.get(parts.size() - 1);
            if ("\n".equals(last.getString())) {
                parts.remove(parts.size() - 1);
            }
        }

        // Optional cost annotation
        if (config.isShowCost && usageIsPresent) {
            parts.add(buildCostComponent(tokenUsed, cost));
        }

        // ── 2. Flatten into a single Component ───────────────────────────────
        MutableComponent root = Component.empty();
        for (Component c : parts) {
            root.append(c);
        }
        return root;
    }

    // ── Top-level parser (handles fenced code blocks) ────────────────────────

    /**
     * Splits {@code text} on fenced code blocks, delegating segments between
     * them to {@link #parseMarkdownText} and wrapping code blocks directly.
     */
    private static List<Component> parseTopLevel(String text, Config config) {
        List<Component> result = new ArrayList<>();
        int cursor = 0;

        Matcher blockMatcher = BLOCK_CODE.matcher(text);
        while (blockMatcher.find()) {
            // Text before this code block
            if (blockMatcher.start() > cursor) {
                result.addAll(parseMarkdownText(text.substring(cursor, blockMatcher.start()), config));
            }
            // The code block itself
            result.add(buildCodeBlock(blockMatcher.group(1)));
            cursor = blockMatcher.end();
        }

        // Remaining text after the last code block (or the whole string)
        if (cursor < text.length()) {
            result.addAll(parseMarkdownText(text.substring(cursor), config));
        }

        return result;
    }

    // ── Second-level parser (handles <think> tags) ───────────────────────────

    /**
     * Scans {@code text} for {@code <think>} tags, handles them, and delegates
     * everything else to {@link #parseLines}.
     */
    private static List<Component> parseMarkdownText(String text, Config config) {
        List<Component> result = new ArrayList<>();

        Matcher thinkMatcher = THINK_TAG.matcher(text);
        int cursor = 0;

        while (thinkMatcher.find()) {
            // Content before the tag
            if (thinkMatcher.start() > cursor) {
                result.addAll(parseLines(text.substring(cursor, thinkMatcher.start())));
            }
            // The think tag itself
            if (config.isThinkModel) {
                result.add(buildThinkComponent(thinkMatcher.group(1)));
            }
            cursor = thinkMatcher.end();
            // Skip any leading whitespace that follows the closing tag
            while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
                cursor++;
            }
        }

        // Remaining content
        if (cursor < text.length()) {
            result.addAll(parseLines(text.substring(cursor)));
        }

        return result;
    }

    // ── Line-by-line parser ───────────────────────────────────────────────────

    /**
     * Splits {@code text} into lines and converts each one into
     * {@link Component}s separated by newline literals.
     */
    private static List<Component> parseLines(String text) {
        List<Component> result = new ArrayList<>();
        String[] lines = text.split("\\r?\\n", -1);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                // Preserve blank lines as paragraph separators (except at end)
                if (i < lines.length - 1) {
                    result.add(Component.literal("\n"));
                }
                continue;
            }
            result.addAll(parseLine(line));
            result.add(Component.literal("\n"));
        }
        return result;
    }

    // ── Per-line inline parser ────────────────────────────────────────────────

    /**
     * Parses a single line of markdown, applying heading / bold / italic /
     * underline / strikethrough / link and inline-code formatting in order.
     *
     * <p>Each pattern is tried as a <em>whole-line</em> match first (headings),
     * then as an inline match (everything else).
     */
    private static List<Component> parseLine(String line) {
        // ── Headings (whole-line matches) ─────────────────────────────────────
        Matcher m;

        m = H3.matcher(line);
        if (m.matches()) {
            return List.of(styledLiteral(m.group(1),
                    s -> s.withColor(TextColor.fromRgb(COLOR_H3)).withBold(true)));
        }
        m = H2.matcher(line);
        if (m.matches()) {
            return List.of(styledLiteral(m.group(1),
                    s -> s.withColor(TextColor.fromRgb(COLOR_H2)).withBold(true)));
        }
        m = H1.matcher(line);
        if (m.matches()) {
            return List.of(styledLiteral(m.group(1),
                    s -> s.withColor(TextColor.fromRgb(COLOR_H1)).withBold(true)));
        }

        // ── Inline spans ──────────────────────────────────────────────────────
        // We walk through the line, applying each inline pattern in priority order.
        // "Segments" alternate between plain-text runs and styled spans.
        return parseInlineSpans(line);
    }

    /**
     * Applies inline markdown patterns (bold → italic → underline → strike →
     * link → inline-code) iteratively until no more matches are found,
     * then returns a list of {@link Component}s.
     *
     * <p>Strategy: repeatedly find the <em>leftmost</em> match of any inline
     * pattern, emit any preceding plain text, emit the styled span, advance the
     * cursor, and repeat.
     */
    private static List<Component> parseInlineSpans(String text) {
        List<Component> result = new ArrayList<>();
        int cursor = 0;

        // Create Matchers once and reuse them via find(start) – avoids per-iteration
        // allocation and the O(n²) rescanning that would occur if we reconstructed
        // them each loop.
        Matcher[] candidates = {
                BOLD.matcher(text),
                ITALIC.matcher(text),
                UNDERLINE.matcher(text),
                STRIKE.matcher(text),
                LINK.matcher(text),
                INLINE_CODE.matcher(text),
        };

        while (cursor < text.length()) {
            // Find the earliest match among all inline patterns
            int bestStart = Integer.MAX_VALUE;
            Matcher bestMatcher = null;
            int bestKind = -1;

            for (int kind = 0; kind < candidates.length; kind++) {
                Matcher cand = candidates[kind];
                if (cand.find(cursor) && cand.start() < bestStart) {
                    bestStart = cand.start();
                    bestMatcher = cand;
                    bestKind = kind;
                }
            }

            if (bestMatcher == null) {
                // No more inline patterns – emit the rest as plain text
                result.add(Component.literal(text.substring(cursor)));
                break;
            }

            // Emit plain text before the match
            if (bestStart > cursor) {
                result.add(Component.literal(text.substring(cursor, bestStart)));
            }

            // Emit the styled span
            result.add(buildInlineSpan(bestKind, bestMatcher));
            cursor = bestMatcher.end();
        }

        return result;
    }

    /** Creates the appropriate styled {@link Component} for an inline span. */
    private static Component buildInlineSpan(int kind, Matcher m) {
        return switch (kind) {
            case 0 -> styledLiteral(m.group(1), s -> s.withBold(true));
            case 1 -> styledLiteral(m.group(1), s -> s.withItalic(true));
            case 2 -> styledLiteral(m.group(1), s -> s.withUnderlined(true));
            case 3 -> styledLiteral(m.group(1), s -> s.withStrikethrough(true));
            case 4 -> buildLinkSpan(m.group(1), m.group(2));
            case 5 -> buildCodeInline(m.group(1));
            default -> Component.literal(m.group(0));
        };
    }

    /**
     * Builds a clickable link component.
     * If the URL is malformed (e.g. contains unencoded spaces from LLM output),
     * {@link URI#create} would throw and crash the renderer.
     * We fall back to plain underlined text so the message is still readable.
     */
    private static Component buildLinkSpan(String label, String rawUrl) {
        MutableComponent base = Component.literal(label)
                .withStyle(s -> s
                        .withColor(TextColor.fromRgb(COLOR_LINK))
                        .withUnderlined(true));
        try {
            // new URI(String) percent-encodes illegal chars; URI.create() does not.
            URI uri = new URI(rawUrl);
            return base.withStyle(s -> s.withClickEvent(new ClickEvent.OpenUrl(uri)));
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            // Malformed URL – render as styled text without a click event.
            return base;
        }
    }

    private static Component buildCodeBlock(String code) {
        return Component.literal(code)
                .withStyle(s -> s
                        .withColor(TextColor.fromRgb(COLOR_CODE))
                        .withFont(FontDescription.DEFAULT));
    }

    private static Component buildCodeInline(String code) {
        return Component.literal(code)
                .withStyle(s -> s
                        .withColor(TextColor.fromRgb(COLOR_CODE))
                        .withFont(FontDescription.DEFAULT));
    }

    private static Component buildThinkComponent(String reason) {
        return Component.literal("[think] ")
                .setStyle(Style.EMPTY.withHoverEvent(
                        new HoverEvent.ShowText(
                                Component.translatable("mcllm.reply.think").append(reason)
                        )
                ));
    }

    private static Component buildCostComponent(long tokenUsed, float cost) {
        return Component.literal(" (Costs: $" + cost + ")")
                .setStyle(Style.EMPTY.withHoverEvent(
                        new HoverEvent.ShowText(
                                Component.translatable("mcllm.token.usage", tokenUsed, cost)
                        )
                ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @FunctionalInterface
    private interface StyleTransformer {
        Style apply(Style s);
    }

    private static MutableComponent styledLiteral(String text, StyleTransformer transformer) {
        return Component.literal(text).withStyle(transformer::apply);
    }
}
