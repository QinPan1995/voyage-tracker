package com.youniverse.voyagetracker.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import com.youniverse.voyagetracker.exception.TrackingException;
import com.youniverse.voyagetracker.model.cma.CmaCgmMovementEvent;
import com.youniverse.voyagetracker.model.cma.CmaCgmTrackingResult;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CmaCgmService implements DisposableBean {

    private static final String SEARCH_URL = "https://www.cma-cgm.com/ebusiness/tracking/search";
    private static final String CDP_URL = "http://127.0.0.1:9222";
    private static final Pattern CMA_DATE_PATTERN = Pattern.compile("(\\d{2})-(\\w{3})-(\\d{4})");
    private static final Map<String, String> MONTH_MAP = new HashMap<>();

    private Playwright playwright;
    private Browser browser;

    static {
        MONTH_MAP.put("JAN", "01"); MONTH_MAP.put("FEB", "02"); MONTH_MAP.put("MAR", "03");
        MONTH_MAP.put("APR", "04"); MONTH_MAP.put("MAY", "05"); MONTH_MAP.put("JUN", "06");
        MONTH_MAP.put("JUL", "07"); MONTH_MAP.put("AUG", "08"); MONTH_MAP.put("SEP", "09");
        MONTH_MAP.put("OCT", "10"); MONTH_MAP.put("NOV", "11"); MONTH_MAP.put("DEC", "12");
    }

    @PostConstruct
    public void init() {
        try {
            this.playwright = Playwright.create();
            this.browser = playwright.chromium().connectOverCDP(CDP_URL);
        } catch (Exception e) {
            throw new TrackingException("Failed to connect Chrome via CDP at " + CDP_URL
                    + ". Ensure Chrome is running with --remote-debugging-port=9222", e);
        }
    }

    public CmaCgmTrackingResult queryTracking(String reference) {
        Page page = getOrCreatePage();
        return doQuery(page, reference);
    }

    private Page getOrCreatePage() {
        List<Page> pages = browser.contexts().get(0).pages();
        return pages.isEmpty() ? browser.contexts().get(0).newPage() : pages.get(0);
    }

    @SuppressWarnings("unchecked")
    private CmaCgmTrackingResult doQuery(Page page, String reference) {
        page.navigate(SEARCH_URL, new Page.NavigateOptions()
                .setWaitUntil(WaitUntilState.NETWORKIDLE));
        page.fill("input[name='SearchViewModel.Reference']", reference);
        page.keyboard().press("Enter");
        page.waitForSelector("#gridTrackingDetails",
                new Page.WaitForSelectorOptions());
        expandGrid(page);
        page.waitForSelector(".k-detail-cell",
                new Page.WaitForSelectorOptions());

        String containerNo = extractText(page,
                "() => { const el = document.querySelector('.resume-filter strong'); return el ? el.textContent.trim() : ''; }");
        String pol = extractText(page,
                "() => { const items = document.querySelectorAll('.timeline--item-description strong'); return items.length > 0 ? items[0]?.textContent.trim() : ''; }");
        String pod = extractText(page,
                "() => { const items = document.querySelectorAll('.timeline--item-description strong'); return items.length > 1 ? items[1]?.textContent.trim() : ''; }");

        List<Map<String, String>> rawRows = (List<Map<String, String>>) page.evaluate(
                "() => {" +
                "  const detail = document.querySelector('.k-detail-cell');" +
                "  if (!detail) return [];" +
                "  const grid = detail.querySelector('[data-role=\"grid\"]');" +
                "  if (!grid) return [];" +
                "  const table = grid.querySelector('table');" +
                "  if (!table) return [];" +
                "  const rows = table.querySelectorAll('tbody tr');" +
                "  return Array.from(rows).slice(1).map(row => {" +
                "    const cols = row.querySelectorAll('td');" +
                "    if (cols.length < 3) return null;" +
                "    const dateCell = cols[1];" +
                "    const calendar = dateCell?.querySelector('.calendar');" +
                "    const time = dateCell?.querySelector('.time');" +
                "    const locCell = cols[3];" +
                "    const location = locCell?.querySelector('span:first-child');" +
                "    const terminal = locCell?.querySelector('.terminal-name');" +
                "    return {" +
                "      date: calendar ? calendar.textContent.trim() : ''," +
                "      time: time ? time.textContent.trim() : ''," +
                "      move: cols[2]?.textContent.trim() || ''," +
                "      location: location ? location.textContent.trim() : ''," +
                "      terminal: terminal ? terminal.textContent.trim() : ''," +
                "      vessel: cols[4]?.textContent.trim() || ''" +
                "    };" +
                "  }).filter(r => r !== null);" +
                "}");

        List<CmaCgmMovementEvent> events = rawRows.stream().map(m -> {
            CmaCgmMovementEvent e = new CmaCgmMovementEvent();
            e.setDate(m.get("date"));
            e.setTime(m.get("time"));
            e.setMove(m.get("move"));
            e.setLocation(m.get("location"));
            e.setTerminal(m.get("terminal"));
            e.setVessel(m.get("vessel"));
            return e;
        }).collect(Collectors.toList());

        CmaCgmTrackingResult result = new CmaCgmTrackingResult();
        result.setContainerNo(containerNo);
        result.setBookingRef(reference);
        result.setPol(pol);
        result.setPod(pod);
        result.setMovements(events);

        return result;
    }

    private void expandGrid(Page page) {
        page.evaluate(
                "() => {" +
                "  var links = document.querySelectorAll('[aria-label]');" +
                "  var target = Array.from(links).find(l => " +
                "    l.getAttribute('aria-label') === 'Display Previous Moves'" +
                "  );" +
                "  if (target) target.click();" +
                "}");
    }

    private String extractText(Page page, String js) {
        Object result = page.evaluate(js);
        return result != null ? result.toString() : "";
    }

    public String convertCmaDate(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        Matcher matcher = CMA_DATE_PATTERN.matcher(raw.trim().toUpperCase());
        if (!matcher.find()) return raw;
        String month = MONTH_MAP.get(matcher.group(2));
        if (month == null) return raw;
        return matcher.group(3) + "-" + month + "-" + matcher.group(1);
    }

    public String extractDateFromDateTime(String dateTime) {
        return convertCmaDate(dateTime);
    }

    @Override
    public void destroy() {
        if (playwright != null) {
            playwright.close();
        }
    }
}
