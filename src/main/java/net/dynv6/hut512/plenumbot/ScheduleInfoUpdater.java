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

package net.dynv6.hut512.plenumbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dynv6.hut512.plenumbot.schedule.ScheduleInfoManager;
import net.dynv6.hut512.plenumbot.schedule.render.ScheduleRenderManagers;
import net.dynv6.hut512.plenumbot.schedule.source.ScheduleSourceManagers;
import net.dynv6.hut512.plenumbot.sql.GuildConfig;
import net.dynv6.hut512.plenumbot.util.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ScheduleInfoUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleInfoUpdater.class);

    private final Timer timer;
    private final ScheduleInfoManager scheduleInfoManager;

    public ScheduleInfoUpdater() {
        this.timer = new Timer("ScheduleInfo info update timer");
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateScheduleInfo();
            }
        }, 0, TimeUnit.HOURS.toMillis(1));
        this.scheduleInfoManager = new ScheduleInfoManager();
        ScheduleRenderManagers.registerRenders(this.scheduleInfoManager);
        ScheduleSourceManagers.registerSources(this.scheduleInfoManager);
    }

    private void updateScheduleInfo() {
        for (Guild guild : PlenumBot.getInstance().getJda().getGuilds()) {
            TextChannel channel = guild.getTextChannelById(GuildConfig.getConfig(guild.getIdLong(), GuildConfig.Config.SCHEDULE_INFO_CHANNEL_ID));
            if (channel == null) continue;

            try {
                ObjectNode options = (ObjectNode) new ObjectMapper().readTree(GuildConfig.getConfig(guild.getIdLong(), GuildConfig.Config.SCHEDULE_INFO_OPTIONS));
                options.put("language", GuildConfig.getConfig(guild.getIdLong(), GuildConfig.Config.LANGUAGE));

                BufferedImage img = scheduleInfoManager.createScheduleInfoImage(guild.getIdLong(),
                        GuildConfig.getConfig(guild.getIdLong(), GuildConfig.Config.SCHEDULE_INFO_RENDER_MANAGER),
                        GuildConfig.getConfig(guild.getIdLong(), GuildConfig.Config.SCHEDULE_INFO_SOURCE_MANAGER),
                        options);

                if (img == null) continue;

                channel.getHistory().retrievePast(100).complete().stream()
                        .filter(msg -> msg.getAuthor().getIdLong() == msg.getJDA().getSelfUser().getIdLong())
                        .findFirst()
                        .ifPresent(msg -> msg.delete().queue());

                String format = GuildConfig.getConfig(guild.getIdLong(), GuildConfig.Config.SCHEDULE_INFO_IMAGE_FORMAT);

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(img, format, out);
                channel.sendFiles(FileUpload.fromData(out.toByteArray(), "schedule." + format)).queue();
            } catch (IOException e) {
                LOGGER.error("Nachricht", e);
                channel.sendMessage(TranslationService.getMessage("unknownError", guild.getIdLong())).queue();
            }
        }
    }

    public void shutdown() {
        this.timer.cancel();
        this.scheduleInfoManager.shutdown();
    }
}
