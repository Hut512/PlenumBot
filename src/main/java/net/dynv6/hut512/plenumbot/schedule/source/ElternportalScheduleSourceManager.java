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

package net.dynv6.hut512.plenumbot.schedule.source;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dynv6.hut512.plenumbot.schedule.ScheduleInfo;
import org.apache.http.auth.InvalidCredentialsException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ElternportalScheduleSourceManager implements ScheduleSourceManager {
    private static final String URL_ROOT = "https://gypenz.eltern-portal.org/";
    private static final String URL_LOGIN = "/includes/project/auth/login.php";
    private static final String URL_SCHEDULE = "/service/stundenplan";
    private static final String URL_SUBSTITUTIONS = "/service/vertretungsplan";
    private static final String SESSION_COOKIE_NAME = "PHPSESSID";

    private static final String LAST_EDITED_PATTERN = "'Stand:' dd.MM.yyyy HH:mm:ss";
    private static final String DATE_PATTERN = "E, d.MM.yyyy - 'KW' w";

    @Override
    public void shutdown() {

    }

    @Override
    public String getName() {
        return "elternportal";
    }

    @Override
    public ScheduleInfo loadScheduleInfo(ObjectNode options) throws InvalidCredentialsException, IOException {
        String sessionCookie = login(options);
        Document scheduleDocument = getScheduleDocument(sessionCookie);
        Document substitutionsDocument = getSubstitutionsDocument(sessionCookie);
        ScheduleInfo scheduleInfo = parseScheduleInfo(scheduleDocument);
        insertSubstitutionsFromDocument(scheduleInfo, substitutionsDocument);
        return scheduleInfo;
    }

    private String login(ObjectNode options) throws IOException, InvalidCredentialsException {
        Connection.Response loginFormResponse = Jsoup.connect(URL_ROOT)
                .method(Connection.Method.GET)
                .execute();

        String sessionCookie = loginFormResponse.cookie(SESSION_COOKIE_NAME);

        if (sessionCookie == null) throw new IOException("Initial session cookie is null");

        Document loginForm = loginFormResponse.parse();
        String csrfToken = loginForm.select("input[name=csrf]").val();

        if (csrfToken.equals("")) throw new IOException("CSRF token empty");

        Connection.Response loginResponse = Jsoup.connect(URL_ROOT + URL_LOGIN)
                .data("username", options.get("username").asText())
                .data("password", options.get("password").asText())
                .data("csrf", csrfToken)
                .cookie(SESSION_COOKIE_NAME, sessionCookie)
                .followRedirects(false)
                .method(Connection.Method.POST)
                .execute();

        String redirectLocation = loginResponse.header("Location");
        if (redirectLocation == null || redirectLocation.contains("login")) throw new InvalidCredentialsException();

        sessionCookie = loginResponse.cookie(SESSION_COOKIE_NAME);

        if (sessionCookie == null) throw new IOException("Login session cookie is null");

        return sessionCookie;
    }

    private Document getScheduleDocument(String sessionCookie) throws IOException {
        return Jsoup.connect(URL_ROOT + URL_SCHEDULE)
                .cookie(SESSION_COOKIE_NAME, sessionCookie)
                .get();
    }

    private Document getSubstitutionsDocument(String sessionCookie) throws IOException {
        return Jsoup.connect(URL_ROOT + URL_SUBSTITUTIONS)
                .cookie(SESSION_COOKIE_NAME, sessionCookie)
                .get();
    }

    private ScheduleInfo parseScheduleInfo(Document scheduleDocument) {
        Elements schedule = scheduleDocument.select("table.table.table-condensed.table-bordered tbody").get(0).children();
        schedule.remove(0);

        LocalTime[][] schoolHours = new LocalTime[schedule.size()][2];
        Map<DayOfWeek, ScheduleInfo.Lesson[]> lessons = new HashMap<>();

        for (int i = 0; i < schedule.size(); i++) {
            Elements fields = schedule.get(i).children();

            String timeString = fields.get(0).html();
            timeString = timeString.substring(timeString.indexOf("<br>") + 5);
            String[] timeComponents = timeString.split(" - ");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH.mm");
            LocalTime startTime = LocalTime.parse(timeComponents[0], formatter);
            LocalTime endTime = LocalTime.parse(timeComponents[1], formatter);
            schoolHours[i] = new LocalTime[]{startTime, endTime};

            for (int j = 1; j < fields.size(); j++) {
                Element dataField = fields.get(j).select("span").get(0);
                String[] data = dataField.html().split("<br>");
                if (data.length != 2) continue;
                String subject = data[0];
                String room = data[1];
                lessons.computeIfAbsent(DayOfWeek.of(j), k -> new ScheduleInfo.Lesson[schedule.size() - 1])[i] = new ScheduleInfo.Lesson(subject, room, "");
            }
        }

        return new ScheduleInfo(schoolHours, lessons);
    }

    private void insertSubstitutionsFromDocument(ScheduleInfo scheduleInfo, Document substitutionsDocument) {
        Elements content = substitutionsDocument.select(".main_center").get(0).children();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.GERMAN);

        LocalDate dateToday = LocalDate.parse(content.get(0).text(), dateFormatter);
        DayOfWeek dayToday = dateToday.getDayOfWeek();

        Elements substitutionsToday = content.get(1).select("tr.liste_grau, tr.liste_weiss");
        extractSubstitutions(substitutionsToday, scheduleInfo.getLessons().get(dayToday));


        LocalDate dateTomorrow = LocalDate.parse(content.get(2).text(), dateFormatter);
        DayOfWeek dayTomorrow = dateTomorrow.getDayOfWeek();

        Elements substitutionsTomorrow = content.get(3).select("tr.liste_grau, tr.liste_weiss");
        extractSubstitutions(substitutionsTomorrow, scheduleInfo.getLessons().get(dayTomorrow));

        String lastEditedStr = content.get(4).text();
        DateTimeFormatter lastEditedFormatter = DateTimeFormatter.ofPattern(LAST_EDITED_PATTERN);
        LocalDateTime lastEdited = LocalDateTime.parse(lastEditedStr, lastEditedFormatter);


        scheduleInfo.getLessons().get(dayToday);
        scheduleInfo.getLessons().get(dayTomorrow);
        scheduleInfo.setLastUpdate(lastEdited);
    }

    private void extractSubstitutions(Elements substitutionsOfDay, ScheduleInfo.Lesson[] lessonsOfDay) {
        for (Element substitution : substitutionsOfDay) {
            Elements substitutionContent = substitution.children();
            String lessonStr = substitutionContent.get(0).text();
            int lessonNr = Integer.parseInt(lessonStr.substring(0, lessonStr.length() - 1));
            String teacher = substitutionContent.get(2).text();
            String subject = substitutionContent.get(3).wholeOwnText();
            String room = substitutionContent.get(4).wholeOwnText();
            String info = substitutionContent.get(5).wholeOwnText();
            ScheduleInfo.Lesson lesson = lessonsOfDay[lessonNr - 1];
            lesson.setSubstituteTeacher(teacher);
            lesson.setSubstituteSubject(subject);
            lesson.setSubstituteRoom(room);
            lesson.setInfo(info);
        }
    }
}