package net.dynv6.hut512.plenumbot.elternportal;

import java.io.IOException;
import java.time.LocalDateTime;

public interface ScheduleScraper {
    public void scrape() throws IOException;
    public LocalDateTime getLastEdited();
    public Schedule getSchedule();
}
