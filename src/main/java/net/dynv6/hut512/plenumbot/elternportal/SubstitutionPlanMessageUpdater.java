package net.dynv6.hut512.plenumbot.elternportal;

import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dynv6.hut512.plenumbot.PlenumBot;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class SubstitutionPlanMessageUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubstitutionPlanMessageUpdater.class);
    public SubstitutionPlanMessageUpdater() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                updateEmbeds();
            }
        }, 0, 60 * 60 * 1000);
    }

    private void updateEmbeds() {
        EmbedBuilder eb = new EmbedBuilder();
        try {
            // Connect to the login page
            String channelId = PlenumBot.getInstance().getConfig().getProperty("SUBSTITUTION_CHANNEL_ID");

            TextChannel textChannel = PlenumBot.getInstance().getJda().getTextChannelById(channelId);

            if (textChannel == null) {
                LOGGER.error("No text text channel with id {} found", channelId);
                return;
            }

            Optional<Message> lastMsg = textChannel.getHistory().retrievePast(100).complete().stream()
                    .filter(msg -> msg.getAuthor().getIdLong() == msg.getJDA().getSelfUser().getIdLong())
                    .findFirst();
        } catch (Exception e) {
            //LOGGER.error("Could not scrape {}", URL_BASE, e);
        }
    }

    private MessageEmbed convertDocumentToEmbed(Document document) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Vertretungsplan am " +
                LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
        Elements days = document.select(".table");
        Elements today = days.get(0).select(".liste_grau");
        eb.addField("Heute", today.text(), false);
        eb.addBlankField(false);
        Elements tomorrow = days.get(1).select(".liste_grau");
        eb.addField("Nächster Schultag", tomorrow.text(), false);
        eb.addBlankField(false);
        if (today.isEmpty() && tomorrow.isEmpty())
            eb.setColor(Color.GREEN);
        else
            eb.setColor(Color.ORANGE);
        eb.setFooter("Stand " + getLastEdited(document)
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)));
        return eb.build();
    }

    private LocalDateTime getLastEdited(Document document) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'Stand: 'dd.MM.yyyy HH:mm:ss");
        return LocalDateTime.parse(document.getElementsMatchingText(
                "^Stand:\\s\\d{2}\\.\\d{2}\\.\\d{4}\\s\\d{2}:\\d{2}:\\d{2}$").text(), formatter);
    }
}
