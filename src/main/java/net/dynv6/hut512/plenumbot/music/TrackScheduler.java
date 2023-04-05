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
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import lombok.Getter;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TrackScheduler extends AudioEventAdapter {
    @Getter
    private final AudioPlayer player;
    @Getter
    private final BlockingQueue<AudioTrack> queue;
    private Timer leaveTimer;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedBlockingQueue<>();
        this.leaveTimer = null;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
        //setInactive(false);
    }

    public void nextTrack() {
        player.startTrack(queue.poll(), false);
        //setInactive(false);
    }

    public void setPaused(boolean paused) {
        player.setPaused(paused);
    }

    public void stop() {
        player.stopTrack();
        queue.clear();
        //setInactive(true);
    }

    public void leave() {
        stop();
        //TODO: finish
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            nextTrack();
        }
    }

    @SuppressWarnings("unused")
    private void setInactive(boolean inactive) {
        if (inactive) {
            if (leaveTimer == null) {
                leaveTimer = new Timer();
                leaveTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        leave();
                    }
                }, TimeUnit.MINUTES.toMillis(5));
            }
        } else {
            if (leaveTimer != null) {
                leaveTimer.cancel();
                leaveTimer = null;
            }
        }
    }
}
