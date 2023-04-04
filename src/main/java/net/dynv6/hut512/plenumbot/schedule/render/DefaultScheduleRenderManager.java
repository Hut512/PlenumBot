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

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dynv6.hut512.plenumbot.schedule.ScheduleInfo;
import net.dynv6.hut512.plenumbot.util.TranslationService;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.time.DayOfWeek;
import java.time.format.TextStyle;

public class DefaultScheduleRenderManager implements ScheduleRenderManager {
    private static final int WIDTH = 1920;
    private static final int HEIGHT = 1080;

    @Override
    public void shutdown() {

    }

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public BufferedImage render(ScheduleInfo scheduleInfo, ObjectNode options) {
        if (scheduleInfo == null) return null;

        int numRows = scheduleInfo.getSchoolHours().length + 1;
        int numCols = scheduleInfo.getLessons().keySet().size() + 1;

        String[][] data = new String[numRows][numCols];

        for (int i = 1; i < numCols; i++) {
            data[0][i] = DayOfWeek.of(i).getDisplayName(TextStyle.FULL,
                    TranslationService.languageToLocale(options.get("language").asText()));
        }

        for (int i = 1; i < numRows; i++) {
            data[i][0] = i + ".";
        }

        for (int i = 1; i < numCols; i++) {
            ScheduleInfo.Lesson[] lessons = scheduleInfo.getLessons().get(DayOfWeek.of(i));

            for (int j = 1; j < lessons.length; j++) {
                ScheduleInfo.Lesson lesson = lessons[j - 1];
                if (lesson == null) continue;
                String subject = lesson.getSubject();
                String substituteSubject = lesson.getSubstituteSubject();
                if (substituteSubject == null) {
                    data[j][i] = subject;
                } else {
                    data[j][i] = "~" + subject + "\n" + lesson.getSubstituteSubject();
                }
            }
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = image.createGraphics();

        g2d.setBackground(Color.DARK_GRAY);
        g2d.clearRect(0, 0, WIDTH, HEIGHT);

        DefaultTable table = new DefaultTable(data, WIDTH / 20, HEIGHT / 20, WIDTH - WIDTH / 10, HEIGHT - HEIGHT / 5);
        table.draw(g2d);

        g2d.setFont(new Font("Arial", Font.PLAIN, WIDTH / 160));
        String lastEditedStr = TranslationService.getMessage("lastEdited", options.get("language").asText(), scheduleInfo.getLastUpdate());

        g2d.drawString(lastEditedStr, WIDTH / 20, HEIGHT - HEIGHT / 20);

        g2d.dispose();

        return image;
    }

    @Override
    public boolean shouldReRender(ScheduleInfo oldScheduleInfo, ScheduleInfo newScheduleInfo) {
        return oldScheduleInfo == null || oldScheduleInfo.getLastUpdate().isBefore(newScheduleInfo.getLastUpdate());
    }
}
