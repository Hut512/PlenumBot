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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public class GuildMusicManager {
    private static final Map<Long, GuildMusicManager> guildMusicManagers = new HashMap<>();

    public static GuildMusicManager get(long guildId) {
        return guildMusicManagers.computeIfAbsent(guildId, k -> new GuildMusicManager());
    }

    @Getter
    private final AudioPlayer player;
    @Getter
    private final TrackScheduler scheduler;

    public GuildMusicManager() {
        player = PlenumAudioPlayerManager.getInstance().createPlayer();
        scheduler = new TrackScheduler(player);
        player.addListener(scheduler);
    }

    public AudioTrack getPlayingTrack() {
        return player.getPlayingTrack();
    }

    public Queue<AudioTrack> getAudioQueue() {
        return scheduler.getQueue();
    }

    public AudioPlayerSendHandler getSendHandler() {
        return new AudioPlayerSendHandler(player);
    }
}
