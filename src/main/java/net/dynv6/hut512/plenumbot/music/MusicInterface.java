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

package net.dynv6.hut512.plenumbot.music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dynv6.hut512.plenumbot.PlenumBot;
import net.dynv6.hut512.plenumbot.listener.MusicInterfaceListener;
import net.dynv6.hut512.plenumbot.sql.GuildConfig;
import net.dynv6.hut512.plenumbot.util.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

public class MusicInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicInterfaceListener.class);

    private static final EmbedBuilder EB_TEMPLATE;

    static {
        EB_TEMPLATE = new EmbedBuilder();
        EB_TEMPLATE.setThumbnail("attachment://Note.png");
        EB_TEMPLATE.setColor(Color.CYAN);
        EB_TEMPLATE.setFooter(PlenumBot.VERSION);
    }

    @Getter
    private long guildId;
    @Getter
    private long channelId;
    private long messageId;

    public MusicInterface(long guildId, long channelId, boolean clearChannel) {
        this.guildId = guildId;
        this.channelId = channelId;
        removeMessages(clearChannel);
        updateMessage();
    }

    private void removeMessages(boolean clearChannel) {
        TextChannel channel = PlenumBot.getInstance().getJda().getTextChannelById(channelId);
        if (channel == null) return;
        for (Message msg : channel.getIterableHistory()) {
            if (clearChannel || msg.getAuthor() == PlenumBot.getInstance().getJda().getSelfUser())
                msg.delete().queue();
        }
    }

    public void updateMessage() {
        TextChannel channel = PlenumBot.getInstance().getJda().getTextChannelById(channelId);
        if (channel == null) return;

        String language = GuildConfig.getConfig(guildId, GuildConfig.Config.LANGUAGE);

        channel.getHistory().retrievePast(100).queue(msgs -> {
            Optional<Message> msg = msgs.stream().filter(m -> m.getAuthor().getIdLong() == m.getJDA().getSelfUser().getIdLong()).findFirst();
            if (msg.isEmpty()) {
                sendMessage(channel, language);
            } else {
                editMessage(msg.get(), language);
            }
        });
    }

    private void sendMessage(TextChannel channel, String language) {
        MessageEmbed embed = createEmbed(language);

        MessageCreateAction action = channel.sendMessageEmbeds(embed);

        InputStream in = MusicInterface.class.getResourceAsStream("/image/Note.png");
        Objects.requireNonNull(in, "Resource input stream is null");
        action.addFiles(FileUpload.fromData(in, "Note.png"));

        action.setActionRow(getComponents());

        action.queue(msg -> messageId = msg.getIdLong());
    }

    private void editMessage(Message msg, String language) {
        msg.editMessageComponents(ActionRow.of(getComponents())).queue();
        msg.editMessageEmbeds(createEmbed(language)).queue();
    }

    private List<Button> getComponents() {
        GuildMusicManager musicManager = GuildMusicManager.get(guildId);
        return List.of(
                Button.secondary("back", Emoji.fromUnicode("⏮")).withDisabled(musicManager.getPlayingTrack() == null),
                Button.secondary("rewind", Emoji.fromUnicode("⏪")).withDisabled(musicManager.getPlayingTrack() == null),
                Button.secondary(musicManager.getPlayer().isPaused() ? "play" : "pause",
                        musicManager.getPlayer().isPaused() ? Emoji.fromUnicode("▶") : Emoji.fromUnicode("⏸")).withDisabled(musicManager.getPlayingTrack() == null),
                Button.secondary("fast-forward", Emoji.fromUnicode("⏩")).withDisabled(musicManager.getPlayingTrack() == null),
                Button.secondary("continue", Emoji.fromUnicode("⏭")).withDisabled(musicManager.getAudioQueue().isEmpty())
        );
    }

    private MessageEmbed createEmbed(String language) {
        EmbedBuilder eb = new EmbedBuilder(EB_TEMPLATE);
        eb.setTitle(TranslationService.getMessage("musicInterfaceTitle", language), PlenumBot.URL);
        eb.setDescription(TranslationService.getMessage("musicInterfaceDescription", language));

        GuildMusicManager musicManager = GuildMusicManager.get(guildId);
        AudioTrack currentTrack = musicManager.getPlayingTrack();
        if (currentTrack != null) {
            eb.addField(TranslationService.getMessage("musicInterfacePlaying", language),
                    currentTrack.getInfo().title + " - " + currentTrack.getInfo().author, false);
            Queue<AudioTrack> queue = musicManager.getAudioQueue();
            int i = 0;
            for (AudioTrack audioTrack : queue) {
                eb.addField(i + 1 + ".", audioTrack.getInfo().title, false);
                if (++i > 9) break;
            }
        }
        return eb.build();
    }
}
