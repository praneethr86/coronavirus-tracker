package com.codeprady.tracker.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.codeprady.tracker.models.LocationStats;

@Service
public class CoronaVirusDataService {

	private String VIRUS_DATA_URL = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_time_series/time_series_covid19_confirmed_global.csv";
	private List<LocationStats> allStats = new ArrayList<>();
	
	public List<LocationStats> getAllStats() {
		return allStats;
	}

	@PostConstruct
	@Scheduled(cron = "* * 1 * * *") // sec min hr day mon yr
	public void fetchVirusData() throws IOException, MalformedURLException, ProtocolException {
		
		List<LocationStats> newStats = new ArrayList<>();
		
		URL url = new URL(VIRUS_DATA_URL);
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		StringBuilder content;
		try (BufferedReader read = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
			String line;
			content = new StringBuilder();
			while ((line = read.readLine()) != null) {
				content.append(line);
				content.append(System.lineSeparator());
			}

			// csv parsing
			StringReader csvBodyReader = new StringReader(content.toString());
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(csvBodyReader);

			for (CSVRecord record : records) {
				LocationStats locationStat = new LocationStats(); 
				locationStat.setState(record.get("Province/State"));
				locationStat.setCountry(record.get("Country/Region"));
				
				int latestCases = Integer.parseInt(record.get(record.size() - 1));
				int prevDayCases = Integer.parseInt(record.get(record.size() - 2));
				locationStat.setLatestTotalCases(latestCases);
				locationStat.setDiffFromPrevDay(latestCases - prevDayCases);

				newStats.add(locationStat);
			}
			this.allStats = newStats;
		} finally {
			con.disconnect();
		}

	}
}
