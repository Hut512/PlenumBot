/*
 * This file is a part of the PlenumBot software.
 *
 * Copyright (c) 2023  Hut512
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.dynv6.hut512.plenumbot.schedule.render;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public class DefaultTable {
    private final String[][] data;
    private final int xOffset;
    private final int yOffset;
    private final int width;
    private final int height;

    private final Font FONT_DEFAULT;
    private final Font FONT_LARGE;
    private final Map<TextAttribute, Object> ATTRIBUTES_NORMAL;
    private final Map<TextAttribute, Object> ATTRIBUTES_STRIKETHROUGH;

    public DefaultTable(String[][] data, int xOffset, int yOffset, int width, int height) {
        this.data = data;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;

        this.FONT_DEFAULT = new Font("Arial", Font.BOLD, width / 80);
        this.FONT_LARGE = new Font("Arial", Font.BOLD, width / 60);
        this.ATTRIBUTES_NORMAL = new HashMap<>();
        this.ATTRIBUTES_NORMAL.put(TextAttribute.STRIKETHROUGH, false);
        this.ATTRIBUTES_STRIKETHROUGH = new HashMap<>();
        this.ATTRIBUTES_STRIKETHROUGH.put(TextAttribute.STRIKETHROUGH, true);
    }

    public void draw(Graphics2D g2d) {
        int numRows = data.length;
        int numCols = data[0].length;

        int rowHeight = height / numRows;
        int colWidth = width / numCols;

        g2d.setFont(FONT_LARGE);

        for (int i = 0; i < numCols; i++) {
            if (data[0][i] == null) continue;
            int colX = i * colWidth;
            drawStringCentered(g2d, data[0][i], colX, 0, colWidth, rowHeight);
            drawLine(g2d, colX, 0, colX, height);
        }

        g2d.setFont(FONT_DEFAULT);

        for (int i = 1; i < numRows; i++) {
            int rowY = i * rowHeight;
            drawLine(g2d, 0, rowY, width, rowY);
            for (int j = 0; j < numCols; j++) {
                if (data[i][j] == null) continue;
                drawStringCentered(g2d, data[i][j], j * colWidth, rowY, colWidth, rowHeight);
            }
        }
    }

    private void drawStringCentered(Graphics2D g2d, String str, int x, int y, int width, int height) {
        String[] lines = str.split("\n");
        int numLines = lines.length;
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textHeight = (int) (g2d.getFont().getSize() * 1.33);
        for (int i = 0; i < numLines; i++) {
            String line = lines[i];

            if (line.startsWith("~"))
                g2d.setFont(g2d.getFont().deriveFont(ATTRIBUTES_STRIKETHROUGH));
            else
                g2d.setFont(g2d.getFont().deriveFont(ATTRIBUTES_NORMAL));

            line = line.replace("~", "");

            int textWidth = fontMetrics.stringWidth(line);
            g2d.drawString(line, x + (width - textWidth) / 2 + xOffset, y + textHeight / 2 + height / (numLines + 1) * (i + 1) + yOffset);
            // 1: height
            // 2:
        }
    }

    private void drawLine(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        g2d.drawLine(x1 + xOffset, y1 + yOffset, x2 + xOffset, y2 + yOffset);
    }
}
