/*
 * Copyright 2026 Toshiki Iga
 * SPDX-License-Identifier: Apache-2.0
 */
package jp.igapyon.mikuscore.scorefeatures;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

public final class ScoreTies {
    private ScoreTies() {
    }

    public static String buildMusicXmlTieItemsXml(TieState state) {
        TieState safe = state == null ? new TieState(false, false, false, false) : state;
        return (safe.isTieStop() ? "<tie type=\"stop\"/>" : "")
                + (safe.isTieStart() ? "<tie type=\"start\"/>" : "");
    }

    public static String buildMusicXmlTiedItemsXml(TieState state) {
        TieState safe = state == null ? new TieState(false, false, false, false) : state;
        return (safe.isTiedStop() ? "<tied type=\"stop\"/>" : "")
                + (safe.isTiedStart() ? "<tied type=\"start\"/>" : "");
    }

    public static TieState extractMusicXmlTieState(Element note) {
        MutableTieState state = new MutableTieState();
        if (note == null) {
            return state.toTieState();
        }
        for (Node child = note.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element)) {
                continue;
            }
            Element element = (Element) child;
            if ("tie".equals(element.getTagName())) {
                String type = normalizeTieType(element.getAttribute("type"));
                if ("start".equals(type)) {
                    state.tieStart = true;
                } else if ("stop".equals(type)) {
                    state.tieStop = true;
                }
            } else if ("notations".equals(element.getTagName())) {
                collectTiedState(element, state);
            }
        }
        return state.toTieState();
    }

    private static void collectTiedState(Element notations, MutableTieState state) {
        for (Node child = notations.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && "tied".equals(((Element) child).getTagName())) {
                String type = normalizeTieType(((Element) child).getAttribute("type"));
                if ("start".equals(type)) {
                    state.tiedStart = true;
                } else if ("stop".equals(type)) {
                    state.tiedStop = true;
                }
            }
        }
    }

    private static String normalizeTieType(String raw) {
        String normalized = raw == null ? "" : raw.trim().toLowerCase();
        return "start".equals(normalized) || "stop".equals(normalized) ? normalized : null;
    }

    private static final class MutableTieState {
        private boolean tieStart;
        private boolean tieStop;
        private boolean tiedStart;
        private boolean tiedStop;

        private TieState toTieState() {
            return new TieState(tieStart, tieStop, tiedStart, tiedStop);
        }
    }

    public static final class TieState {
        private final boolean tieStart;
        private final boolean tieStop;
        private final boolean tiedStart;
        private final boolean tiedStop;

        public TieState(boolean tieStart, boolean tieStop, boolean tiedStart, boolean tiedStop) {
            this.tieStart = tieStart;
            this.tieStop = tieStop;
            this.tiedStart = tiedStart;
            this.tiedStop = tiedStop;
        }

        public boolean isTieStart() {
            return tieStart;
        }

        public boolean isTieStop() {
            return tieStop;
        }

        public boolean isTiedStart() {
            return tiedStart;
        }

        public boolean isTiedStop() {
            return tiedStop;
        }
    }
}
