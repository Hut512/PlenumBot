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

package net.dynv6.hut512.plenumbot.util;

import net.dynv6.hut512.plenumbot.sql.GuildConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class TranslationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TranslationService.class);

    public static String getMessage(String key, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", locale);
        String msg;
        try {
            msg = bundle.getString(key);
        } catch (MissingResourceException e) {
            try {
                bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
                msg = bundle.getString(key);
            } catch (MissingResourceException e1) {
                LOGGER.warn("Could not translate " + key, e1);
                msg = key;
            }
        }
        return msg;
    }

    public static String getMessage(String key, Locale locale, Object... params) {
        return MessageFormat.format(getMessage(key, locale), params);
    }

    public static String getMessage(String key, String language, Object... params) {
        return getMessage(key, languageToLocale(language), params);
    }

    public static String getMessage(String key, long guild, Object... params) {
        return getMessage(key, GuildConfig.getConfig(guild, GuildConfig.Config.LANGUAGE), params);
    }

    public static Locale languageToLocale(String language) {
        //For some reason Locale.of() throws "java.lang.NoSuchMethodError: 'java.util.Locale java.util.Locale.of(java.lang.String)'"
        return language == null ? Locale.ENGLISH : new Locale(language);
    }
}
