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

package net.dynv6.hut512.plenumbot.schedule;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dynv6.hut512.plenumbot.schedule.render.ScheduleRenderManager;
import net.dynv6.hut512.plenumbot.schedule.source.ScheduleSourceManager;
import net.dynv6.hut512.plenumbot.util.TranslationService;
import org.apache.http.auth.InvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ScheduleInfoManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleInfoManager.class);

    private final Map<String, ScheduleRenderManager> renderManagers;
    private final Map<String, ScheduleSourceManager> sourceManagers;
    private final Map<Long, ScheduleInfo> oldScheduleInfos;

    public ScheduleInfoManager() {
        this.renderManagers = new HashMap<>();
        this.sourceManagers = new HashMap<>();
        this.oldScheduleInfos = new HashMap<>();
    }

    public void registerRenderManager(ScheduleRenderManager renderManager) {
        this.renderManagers.put(renderManager.getName(), renderManager);
    }

    public void registerSourceManager(ScheduleSourceManager sourceManager) {
        this.sourceManagers.put(sourceManager.getName(), sourceManager);
    }

    public BufferedImage createScheduleInfoImage(long guild, String renderManagerName, String sourceManagerName, ObjectNode options) {
        ScheduleRenderManager renderManager = this.renderManagers.get(renderManagerName);
        ScheduleSourceManager sourceManager = this.sourceManagers.get(sourceManagerName);

        if (renderManager == null) {
            return createErrorImage(TranslationService.getMessage("invalidValueError", options.get("language").asText(), "Renderer", renderManagerName));
        }

        if (sourceManager == null) {
            return createErrorImage(TranslationService.getMessage("invalidValueError", options.get("language").asText(), "SourceManager", sourceManagerName));
        }

        ScheduleInfo scheduleInfo;
        try {
            scheduleInfo = sourceManager.loadScheduleInfo(options);
        } catch (InvalidCredentialsException e) {
            LOGGER.debug("Invalid credentials for " + sourceManager.getName(), e);
            return createErrorImage(TranslationService.getMessage("invalidCredentialsError", options.get("language").asText(), sourceManager.getName()));
        } catch (IOException e) {
            LOGGER.warn("Could not load scheduleInfo from " + sourceManager.getName(), e);
            return createErrorImage(TranslationService.getMessage("unknownError", options.get("language").asText()));
        }

        ScheduleInfo oldScheduleInfo = oldScheduleInfos.get(guild);

        oldScheduleInfos.put(guild, scheduleInfo);

        if (!renderManager.shouldReRender(oldScheduleInfo, scheduleInfo)) return null;

        return renderManager.render(scheduleInfo, options);
    }

    private BufferedImage createErrorImage(String message) {
        BufferedImage image = new BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = image.createGraphics();

        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.setColor(Color.RED);

        String[] lines = message.split("\n");
        int lineHeight = g2d.getFontMetrics().getHeight();
        int y = 50;
        for (String line : lines) {
            int stringWidth = g2d.getFontMetrics().stringWidth(line);
            int x = (image.getWidth() - stringWidth) / 2;
            g2d.drawString(line, x, y);
            y += lineHeight;
        }

        g2d.dispose();

        return image;
    }

    public void shutdown() {
        this.renderManagers.values().forEach(ScheduleRenderManager::shutdown);
        this.sourceManagers.values().forEach(ScheduleSourceManager::shutdown);
    }
}
