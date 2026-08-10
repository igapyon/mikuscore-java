/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreArticulations {
    private static final List<String> ARTICULATION_KINDS = Arrays.asList("staccato", "staccatissimo", "accent",
            "tenuto", "strong-accent", "breath-mark", "caesura");
    private static final Set<String> ARTICULATION_KIND_SET = new LinkedHashSet<String>(ARTICULATION_KINDS);

    private ScoreArticulations() {
    }

    public static String normalizeArticulationKind(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        return ARTICULATION_KIND_SET.contains(normalized) ? normalized : null;
    }

    public static String buildMusicXmlArticulationItemsXml(Iterable<String> kinds) {
        StringBuilder xml = new StringBuilder();
        for (String kind : uniqueNormalizedKinds(kinds)) {
            xml.append("<").append(kind).append("/>");
        }
        return xml.toString();
    }

    public static String buildMusicXmlArticulationsXml(Iterable<String> kinds) {
        String itemsXml = buildMusicXmlArticulationItemsXml(kinds);
        return itemsXml.isEmpty() ? "" : "<articulations>" + itemsXml + "</articulations>";
    }

    public static List<String> extractMusicXmlArticulationKinds(Element note) {
        List<String> out = new ArrayList<String>();
        if (note == null) {
            return out;
        }
        for (Node notationNode = note.getFirstChild(); notationNode != null; notationNode = notationNode
                .getNextSibling()) {
            if (!(notationNode instanceof Element) || !"notations".equals(((Element) notationNode).getTagName())) {
                continue;
            }
            Element notations = (Element) notationNode;
            for (Node articulationsNode = notations.getFirstChild(); articulationsNode != null; articulationsNode = articulationsNode
                    .getNextSibling()) {
                if (!(articulationsNode instanceof Element)
                        || !"articulations".equals(((Element) articulationsNode).getTagName())) {
                    continue;
                }
                collectDirectArticulationKinds((Element) articulationsNode, out);
            }
        }
        return uniqueNormalizedKinds(out);
    }

    private static void collectDirectArticulationKinds(Element articulations, List<String> out) {
        for (Node child = articulations.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element) {
                String kind = normalizeArticulationKind(((Element) child).getTagName());
                if (kind != null) {
                    out.add(kind);
                }
            }
        }
    }

    private static List<String> uniqueNormalizedKinds(Iterable<String> kinds) {
        LinkedHashSet<String> unique = new LinkedHashSet<String>();
        if (kinds != null) {
            for (String kind : kinds) {
                String normalized = normalizeArticulationKind(kind);
                if (normalized != null) {
                    unique.add(normalized);
                }
            }
        }
        return new ArrayList<String>(unique);
    }
}
