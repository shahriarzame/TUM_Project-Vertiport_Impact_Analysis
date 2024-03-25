package net.bhl.matsim.uam.analysis.traveltimes.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.contrib.util.CSVReaders;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TripItemReader {
	public static List<TripItem> getTripItems(String tripsInput) throws IOException {
		List<TripItem> trips = new ArrayList<>();
		List<String[]> rows = CSVReaders.readCSV(tripsInput);
 		for (String[] row : rows.subList(1, rows.size())) {
 			int j = 0;
			TripItem trip = new TripItem();
			trip.origin = new Coord(Double.parseDouble(row[15]), Double.parseDouble(row[16]));
			trip.destination = new Coord(Double.parseDouble(row[19]), Double.parseDouble(row[20]));
			trip.departureTime = Time.parseTime(row[3]);
			trips.add(trip);
		}
		return trips;
	}
}
