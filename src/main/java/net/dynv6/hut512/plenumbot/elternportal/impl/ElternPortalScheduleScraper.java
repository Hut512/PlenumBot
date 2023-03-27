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

    @Getter LocalDateTime lastEdited;
    @Getter Schedule schedule;

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
