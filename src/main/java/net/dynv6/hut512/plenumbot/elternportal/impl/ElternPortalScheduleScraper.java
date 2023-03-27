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

package net.dynv6.hut512.plenumbot.elternportal.impl;

import lombok.Getter;
import net.dynv6.hut512.plenumbot.PlenumBot;
import net.dynv6.hut512.plenumbot.elternportal.Schedule;
import net.dynv6.hut512.plenumbot.elternportal.ScheduleScraper;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.time.LocalDateTime;

public class ElternPortalScheduleScraper implements ScheduleScraper {
    private static final String URL_BASE = "https://gypenz.eltern-portal.org/";
    private static final String URL_LOGIN = "includes/project/auth/login.php";
    private static final String URL_SUBSTITUTIONS = "service/vertretungsplan";
    private static final String URL_SCHEDULE = "service/stundenplan";

    private final String username;
    private final String password;

    @Getter
    LocalDateTime lastEdited;
    @Getter
    Schedule schedule;

    public ElternPortalScheduleScraper(String username, String password) {
        this.username = username;
        this.password = password;
        this.lastEdited = LocalDateTime.MIN;
        this.schedule = null;
    }

    @Override
    public void scrape() throws IOException {
        Connection.Response loginPage = Jsoup.connect(URL_BASE)
                .execute();

        Document loginDoc = loginPage.parse();
        String loginCsrfToken = loginDoc.select("input[name=csrf]").attr("value");

        Connection.Response loginAction = Jsoup.connect(URL_BASE + URL_LOGIN)
                .data("username", PlenumBot.getInstance().getConfig().getProperty("WEB_USERNAME"))
                .data("password", PlenumBot.getInstance().getConfig().getProperty("WEB_PASSWORD"))
                .data("csrf", loginCsrfToken)
                .referrer(URL_BASE)
                .cookies(loginPage.cookies())
                .method(Connection.Method.POST)
                .followRedirects(false)
                .execute();

        Document substitutionsDocument = Jsoup.connect(URL_BASE + URL_SUBSTITUTIONS)
                .referrer(URL_BASE)
                .cookies(loginAction.cookies())
                .followRedirects(false)
                .get();

        Document scheduleDocument = Jsoup.connect(URL_BASE + URL_SCHEDULE)
                .referrer(URL_BASE)
                .cookies(loginAction.cookies())
                .followRedirects(false)
                .get();
    }
}
