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

package net.dynv6.hut512.plenumbot.listener;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dynv6.hut512.plenumbot.PlenumBot;
import net.dynv6.hut512.plenumbot.music.GuildMusicManager;
import net.dynv6.hut512.plenumbot.music.MusicInterface;
import net.dynv6.hut512.plenumbot.music.PlenumAudioPlayerManager;
import net.dynv6.hut512.plenumbot.sql.GuildConfig;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MusicInterfaceListener extends ListenerAdapter {
    private final Map<Long, MusicInterface> musicInterfaces;

    public MusicInterfaceListener() {
        musicInterfaces = new HashMap<>();
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        for (Guild guild : PlenumBot.getInstance().getJda().getGuilds()) {
            createMusicInterface(guild);
        }
    }

    public void createMusicInterface(Guild guild) {
        String channelIdStr = GuildConfig.getConfig(guild.getIdLong(), GuildConfig.Config.MUSIC_INTERFACE_CHANNEL_ID);
        if (channelIdStr.equals("0")) return;
        musicInterfaces.put(guild.getIdLong(),
                new MusicInterface(guild.getIdLong(), Long.parseLong(channelIdStr),
                        GuildConfig.getConfig(guild.getIdLong(), GuildConfig.Config.MUSIC_INTERFACE_SHOULD_CLEAR_CHANNEL)));
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.isFromGuild() || event.getButton().getId() == null) return;

        switch (event.getButton().getId()) {
            case "back" -> {
                AudioTrack currentTrack = GuildMusicManager.get(event.getGuild().getIdLong()).getPlayer().getPlayingTrack();
                currentTrack.setPosition(0);
                event.reply(":thumbsup:").setEphemeral(true).queue(msg -> msg.deleteOriginal().complete());
            }
            case "rewind" -> {
                AudioTrack currentTrack = GuildMusicManager.get(event.getGuild().getIdLong()).getPlayer().getPlayingTrack();
                currentTrack.setPosition(Math.max(currentTrack.getPosition() - TimeUnit.SECONDS.toMillis(30), 0));
                event.reply(":thumbsup:").setEphemeral(true).queue(msg -> msg.deleteOriginal().complete());
            }
            case "play" -> {
                GuildMusicManager.get(event.getGuild().getIdLong()).getScheduler().setPaused(false);
                event.reply(":thumbsup:").setEphemeral(true).queue(msg -> msg.deleteOriginal().complete());
                musicInterfaces.get(event.getGuild().getIdLong()).updateMessage();
            }
            case "pause" -> {
                GuildMusicManager.get(event.getGuild().getIdLong()).getScheduler().setPaused(true);
                event.reply(":thumbsup:").setEphemeral(true).queue(msg -> msg.deleteOriginal().complete());
                musicInterfaces.get(event.getGuild().getIdLong()).updateMessage();
            }
            case "fast-forward" -> {
                AudioTrack currentTrack = GuildMusicManager.get(event.getGuild().getIdLong()).getPlayer().getPlayingTrack();
                currentTrack.setPosition(Math.min(currentTrack.getPosition() + TimeUnit.SECONDS.toMillis(30), currentTrack.getDuration()));
                event.reply(":thumbsup:").setEphemeral(true).queue(msg -> msg.deleteOriginal().complete());
            }
            case "continue" -> {
                GuildMusicManager.get(event.getGuild().getIdLong()).getScheduler().nextTrack();
                event.reply(":thumbsup:").setEphemeral(true).queue(msg -> msg.deleteOriginal().complete());
                musicInterfaces.get(event.getGuild().getIdLong()).updateMessage();
            }
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor() == event.getJDA().getSelfUser()) return;
        if (!event.getChannel().getId().equals(
                GuildConfig.getConfig(event.getGuild().getIdLong(), GuildConfig.Config.MUSIC_INTERFACE_CHANNEL_ID)))
            return;

        String track = event.getMessage().getContentStripped();
        event.getMessage().delete().queue();

        if (event.getMember() == null ||
                event.getMember().getVoiceState() == null ||
                event.getMember().getVoiceState().getChannel() == null) return;

        VoiceChannel memberVoiceChannel = event.getMember().getVoiceState().getChannel().asVoiceChannel();
        VoiceChannel ownVoiceChannel = event.getGuild().getAudioManager().getConnectedChannel() == null ?
                null : event.getGuild().getAudioManager().getConnectedChannel().asVoiceChannel();

        GuildMusicManager musicManager = GuildMusicManager.get(event.getGuild().getIdLong());

        if (memberVoiceChannel != ownVoiceChannel) {
            if (musicManager.getPlayer().getPlayingTrack() != null) return;
            event.getGuild().getAudioManager().openAudioConnection(memberVoiceChannel);
            event.getGuild().getAudioManager().setSendingHandler(musicManager.getSendHandler());
        }

        PlenumAudioPlayerManager.getInstance().loadItemOrdered(musicManager, track, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                musicManager.getScheduler().queue(track);
                musicInterfaces.get(event.getGuild().getIdLong()).updateMessage();
                //sendMusicSelectorMessage(event.getGuild(), true, track.getInfo().title);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                for (AudioTrack track : playlist.getTracks()) {
                    musicManager.getScheduler().queue(track);
                }
                musicInterfaces.get(event.getGuild().getIdLong()).updateMessage();
            }

            @Override
            public void noMatches() {
                PlenumAudioPlayerManager.getInstance().loadItemOrdered(musicManager, "ytsearch:" + track, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {

                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                        musicManager.getScheduler().queue(playlist.getTracks().get(0));
                        musicInterfaces.get(event.getGuild().getIdLong()).updateMessage();
                    }

                    @Override
                    public void noMatches() {

                    }

                    @Override
                    public void loadFailed(FriendlyException exception) {

                    }
                });
            }

            @Override
            public void loadFailed(FriendlyException exception) {
            }
        });
    }


}
