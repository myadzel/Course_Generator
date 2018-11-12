/*
 * Course Generator
 * Copyright (C) 2016 Pierre Delore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package course_generator.weather;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import course_generator.TrackData;
import course_generator.settings.CgSettings;
import course_generator.utils.CgConst;
import course_generator.utils.CgLog;
import course_generator.utils.Utils;

public class JPanelWeather extends JPanel {
	private static final long serialVersionUID = -7168142806619093218L;
	private ResourceBundle bundle;
	private CgSettings settings = null;
	private JEditorPane editorStat;
	private JScrollPane scrollPaneStat;
	private JToolBar toolBar;
	private JButton btWeatherDataSave;
	private JButton btWeatherRefresh;
	private JLabel lbInformation;
	private JMenuItem InformationWarning;
	private TrackData track = null;

	private Double Latitude;
	private Double Longitude;


	public JPanelWeather(CgSettings settings) {
		super();
		this.settings = settings;
		bundle = java.util.ResourceBundle.getBundle("course_generator/Bundle");
		initComponents();
	}


	private void initComponents() {
		setLayout(new java.awt.BorderLayout());

		// -- Statistic tool bar
		// ---------------------------------------------------
		Create_Statistic_Toolbar();
		add(toolBar, java.awt.BorderLayout.NORTH);

		editorStat = new JEditorPane();
		editorStat.setContentType("text/html");
		editorStat.setEditable(false);
		scrollPaneStat = new JScrollPane(editorStat);
		add(scrollPaneStat, java.awt.BorderLayout.CENTER);
	}


	/**
	 * Create the status toolbar
	 */
	private void Create_Statistic_Toolbar() {
		toolBar = new javax.swing.JToolBar();
		toolBar.setOrientation(javax.swing.SwingConstants.HORIZONTAL);
		toolBar.setFloatable(false);
		toolBar.setRollover(true);

		// -- Save
		// --------------------------------------------------------------
		btWeatherDataSave = new javax.swing.JButton();
		btWeatherDataSave.setIcon(Utils.getIcon(this, "save_html.png", settings.ToolbarIconSize));
		btWeatherDataSave.setToolTipText(bundle.getString("JPanelWeather.btWeatherDataSave.toolTipText"));
		btWeatherDataSave.setFocusable(false);
		btWeatherDataSave.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SaveStat();
			}
		});
		toolBar.add(btWeatherDataSave);

		// -- Separator
		// ---------------------------------------------------------
		toolBar.add(new javax.swing.JToolBar.Separator());

		// -- Refresh
		// --------------------------------------------------------------
		btWeatherRefresh = new javax.swing.JButton();
		btWeatherRefresh.setIcon(Utils.getIcon(this, "refresh.png", settings.ToolbarIconSize));
		btWeatherRefresh.setToolTipText(bundle.getString("JPanelWeather.btWeatherRefresh.toolTipText"));
		btWeatherRefresh.setFocusable(false);
		btWeatherRefresh.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				refresh(track, true);
			}
		});
		toolBar.add(btWeatherRefresh);

		// -- Tab information
		// --------------------------------------------------------------
		InformationWarning = new JMenuItem(Utils.getIcon(this, "cancel.png", settings.TagIconSize));
		InformationWarning.setVisible(false);
		toolBar.add(InformationWarning);

		lbInformation = new JLabel();
		lbInformation.setVisible(false);
		toolBar.add(lbInformation);
	}


	/**
	 * Refresh the statistic tab
	 */
	public void refresh(TrackData track, boolean retrieveOnlineData) {
		if (track == null)
			return;

		if (track.data.isEmpty())
			return;

		this.track = track;

		StringBuilder sb = new StringBuilder();

		// -- Get current language
		String lang = Locale.getDefault().toString();

		InputStream is = getClass().getResourceAsStream("statweather_" + lang + ".html");
		// -- File exist?
		if (is == null) {
			/// -- Use default file
			is = getClass().getResourceAsStream("statweather_en_US.html");
			CgLog.info("RefreshStat: Statistic file not present! Loading the english statistic file");
		}

		try {
			InputStreamReader isr = new InputStreamReader(is, "UTF-8");
			BufferedReader br = new BufferedReader(isr);

			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			br.close();
			isr.close();
			is.close();
		} catch (IOException e) {
			CgLog.error("RefreshStat : Impossible to read the template file from resource");
			e.printStackTrace();
		}

		lbInformation.setText("");
		lbInformation.setVisible(false);
		InformationWarning.setVisible(false);

		ArrayList<WeatherHistory> previousWeatherHistory = new ArrayList<WeatherHistory>();
		if (retrieveOnlineData) {

			if (!Utils.isInternetReachable()) {
				lbInformation.setText(bundle.getString("JPanelWeather.lbInformationMissingInternetConnection.Text"));
				lbInformation.setVisible(true);
				InformationWarning.setVisible(true);
				return;
			}

			if (!settings.isDarkSkyApiKeyValid()) {
				if (lbInformation.getText() != "") {
					lbInformation.setText(", ");
				}
				lbInformation.setText(lbInformation.getText()
						.concat(bundle.getString("JPanelWeather.lbInformationMissingApiKey.Text")));
				lbInformation.setVisible(true);
				InformationWarning.setVisible(true);
				return;
			}

			// TODO if(not online or api key missing, update the label and display the
			// waring)

			for (int forecastIndex = 0; forecastIndex < 3; ++forecastIndex) {
				WeatherHistory previousWeatherData = new WeatherHistory(settings, track, Latitude, Longitude,
						forecastIndex + 1);

				previousWeatherData.RetrieveWeatherData();
				previousWeatherHistory.add(previousWeatherData);
			}
		} else {
			// If exists, get the historical weather from the CGX course
			previousWeatherHistory = track.getHistoricalWeather();
		}

		if (previousWeatherHistory.isEmpty()) {
			// -- Refresh the view and set the cursor position
			editorStat.setText("");
			editorStat.setCaretPosition(0);
			return;
		}
		DateTimeFormatter fmt = DateTimeFormat.forPattern("EE yyyy-MM-dd");

		sb = Utils.sbReplace(sb, "@0", Utils.uTemperatureToString(settings.Unit));
		sb = Utils.sbReplace(sb, "@1", Utils.uSpeed2String(settings.Unit, false));

		for (int totalForecasts = 0; totalForecasts < previousWeatherHistory.size(); ++totalForecasts) {
			WeatherHistory currentPreviousWeatherHistory = previousWeatherHistory.get(totalForecasts);
			WeatherData previousDailyWeather = currentPreviousWeatherHistory.getDailyWeatherData();

			// TODO add a row "Daylight hours??????

			int index = 600 + totalForecasts * 100;

			sb = Utils.sbReplace(sb, "@" + index++,
					fmt.print(Utils.unixTimeToDateTime(previousDailyWeather.getTime())));
			sb = Utils.sbReplace(sb, "@" + index++, addImage(previousDailyWeather.getSummaryIconFilePath()));
			sb = Utils.sbReplace(sb, "@" + index++, previousDailyWeather.getSummary());

			sb = Utils
					.sbReplace(sb, "@" + index++,
							addImage(previousDailyWeather.getThermometerIconFilePath(
									Utils.FormatTemperature(Double.valueOf(previousDailyWeather.getTemperatureHigh()),
											CgConst.UNIT_MILES_FEET))));
			sb = Utils.sbReplace(sb, "@" + index++, displayTemperature(previousDailyWeather.getTemperatureHigh()));
			sb = Utils.sbReplace(sb, "@" + index++, displayTime(previousDailyWeather.getTemperatureHighTime(),
					currentPreviousWeatherHistory.getTimeZone()));

			sb = Utils
					.sbReplace(sb, "@" + index++,
							addImage(previousDailyWeather.getThermometerIconFilePath(
									Utils.FormatTemperature(Double.valueOf(previousDailyWeather.getTemperatureLow()),
											CgConst.UNIT_MILES_FEET))));
			sb = Utils.sbReplace(sb, "@" + index++, displayTemperature(previousDailyWeather.getTemperatureLow()));
			sb = Utils.sbReplace(sb, "@" + index++, displayTime(previousDailyWeather.getTemperatureLowTime(),
					currentPreviousWeatherHistory.getTimeZone()));

			// The wind speed is in meter/second. We convert it first in km/h (1 m/s =
			// 3.6km/h)
			sb = Utils.sbReplace(sb, "@" + index++, Utils.FormatSpeed(
					3.6 * Double.valueOf(previousDailyWeather.getWindSpeed()), settings.Unit, false, false));

			sb = Utils
					.sbReplace(sb, "@" + index++,
							addImage(previousDailyWeather.getThermometerIconFilePath(Utils.FormatTemperature(
									Double.valueOf(previousDailyWeather.getApparentTemperatureHigh()),
									CgConst.UNIT_MILES_FEET))));
			sb = Utils.sbReplace(sb, "@" + index++,
					displayTemperature(previousDailyWeather.getApparentTemperatureHigh()));
			sb = Utils.sbReplace(sb, "@" + index++, displayTime(previousDailyWeather.getApparentTemperatureHighTime(),
					currentPreviousWeatherHistory.getTimeZone()));

			sb = Utils
					.sbReplace(sb, "@" + index++,
							addImage(previousDailyWeather.getThermometerIconFilePath(Utils.FormatTemperature(
									Double.valueOf(previousDailyWeather.getApparentTemperatureLow()),
									CgConst.UNIT_MILES_FEET))));
			sb = Utils.sbReplace(sb, "@" + index++,
					displayTemperature(previousDailyWeather.getApparentTemperatureLow()));
			sb = Utils.sbReplace(sb, "@" + index++, displayTime(previousDailyWeather.getApparentTemperatureLowTime(),
					currentPreviousWeatherHistory.getTimeZone()));

			sb = Utils.sbReplace(sb, "@" + index++, addImage(previousDailyWeather.getPrecipitationTypeIconFilePath()));

			sb = Utils.sbReplace(sb, "@" + index++, String.valueOf(previousDailyWeather.getMoonPhase()));
		}

		// -- Refresh the view and set the cursor position
		editorStat.setText(sb.toString());
		editorStat.setCaretPosition(0);
	}


	public void SetParameters(Double latitude, Double longitude, DateTime startTime) {
		Latitude = latitude;
		Longitude = longitude;
	}


	/**
	 * Save the statistics in TXT format
	 */
	private void SaveStat() {
		String s;
		s = Utils.SaveDialog(this, settings.LastDir, "", ".html", bundle.getString("frmMain.HTMLFile"), true,
				bundle.getString("frmMain.FileExist"));

		if (!s.isEmpty()) {
			try {
				FileWriter out = new FileWriter(s);

				String text = ReplaceImages(editorStat.getText());

				out.write(text);
				out.close();
			} catch (Exception f) {
				CgLog.error("SaveStat : impossible to save the statistic file");
				f.printStackTrace();
			}
			// -- Store the directory
			settings.LastDir = Utils.GetDirFromFilename(s);
		}
	}


	/**
	 * Creates a String containing a temperature value.
	 * 
	 * @param temperatureValue
	 *            The temperature value
	 * @return A String containing a temperature information
	 */
	private String displayTemperature(double temperatureValue) {
		return Utils.FormatTemperature(temperatureValue, settings.Unit) + Utils.uTemperatureToString(settings.Unit);
	}


	/**
	 * Creates a String containing a measured time.
	 * 
	 * @param time
	 *            A Unix time.
	 * @return A String containing a time information.
	 */
	private String displayTime(long time, String timeZone) {
		DateTime dateTime = Utils.unixTimeToDateTime(time, timeZone);

		DateTimeFormatter dtfOut = DateTimeFormat.forPattern("HH:mm");

		return dtfOut.print(dateTime);
	}


	private String addImage(String iconFilePath) {
		if (iconFilePath == "")
			return "";
		return "<img src=\"file:/" + iconFilePath + "\" width=\"50%\" height=\"50%\"/>";
	}


	/**
	 * Because the image paths in the original HTML reference images in the Course
	 * Generator jar (i.e: not accessible by any browser), we convert all the images
	 * references to their actual Base64 value. Why we can't use the Base64
	 * representation in Course Generator : Because Swing's (default) HTML support
	 * does not extend to Base 64 encoded images.
	 * 
	 * @param originalText
	 *            The original HTML page
	 * @return The HTML page containing base64 representations of each image.
	 */
	private String ReplaceImages(String originalText) {
		Document document = Jsoup.parse(originalText);

		document.select("img[src]").forEach(e -> {
			System.out.println(e.text());

			String absoluteFilePath = e.attr("src");

			// We remove the string "file:/"
			absoluteFilePath = absoluteFilePath.substring(6);
			String base64 = Utils.getFileBase64(absoluteFilePath);

			e.attr("src", "data:image/png;base64," + base64);
		});

		return document.toString();
	}
}
