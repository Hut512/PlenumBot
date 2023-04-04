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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@ToString
@Getter
@Setter
@RequiredArgsConstructor
public class ScheduleInfo {
    private LocalDateTime lastUpdate;
    private final LocalTime[][] schoolHours;
    private final Map<DayOfWeek, Lesson[]> lessons;

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class Lesson {
        private final String subject;
        private final String room;
        private final String teacher;

        @Nullable
        private String substituteSubject;
        @Nullable
        private String substituteRoom;
        @Nullable
        private String substituteTeacher;
        @Nullable
        private String info;
    }
}