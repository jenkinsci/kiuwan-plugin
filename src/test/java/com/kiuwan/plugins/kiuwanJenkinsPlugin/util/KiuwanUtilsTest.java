package com.kiuwan.plugins.kiuwanJenkinsPlugin.util;

import static org.junit.Assume.assumeTrue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.AnalysisResult;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.InsightsData;
import com.kiuwan.plugins.kiuwanJenkinsPlugin.model.results.MetricValue;

public class KiuwanUtilsTest {

	private static final String WINDOWS_SCRIPT_FILENAME = "testWindowsEscape.cmd";
	private static final String ARGS_TEST_BATTERY_FILENAME = "testWindowsEscape.txt";
	
	private static Path resourcesPath;
	private static String windowsCmdFile;
	private static List<String> testStrings;
	
	@BeforeClass
	public static void beforeClass() throws URISyntaxException, FileNotFoundException, IOException {
		assumeTrue(System.getProperty("os.name").toLowerCase().startsWith("win"));
		
		String packageName = KiuwanUtilsTest.class.getPackage().getName();
		URL url = KiuwanUtilsTest.class.getResource("/" + packageName.replace(".", "/"));
		resourcesPath = Paths.get(url.toURI());
		windowsCmdFile = resourcesPath.resolve(WINDOWS_SCRIPT_FILENAME).toAbsolutePath().toString();
		
		testStrings = new ArrayList<>();
		try (Stream<String> stream = Files.lines(resourcesPath.resolve(ARGS_TEST_BATTERY_FILENAME))) {
	        stream.forEach(line -> testStrings.add(line));
		}
	}
	
	@Test
	public void testWindowsEscape() throws IOException, URISyntaxException {
		for (String testString : testStrings) {
			System.out.println("-------------");
			System.out.println("Test string \t\t= " + testString);
			String processedTestString = KiuwanUtils.escapeArg(false, testString);
			System.out.println("Processed string \t= " + processedTestString);
			int ret = testCmdCall(testString, processedTestString);
			Assert.assertEquals(0, ret);
		}
	}
	
	@Test
	public void testReadAnalysisResultBaseline() throws IOException {
		try (InputStream is = KiuwanUtilsTest.class.getResourceAsStream("output_baseline.json")) {
			AnalysisResult analysisResult = KiuwanUtils.readAnalysisResult(is);
			
			Assert.assertEquals("CRITICAL", analysisResult.getAnalysisBusinessValue());
			Assert.assertEquals(new Double("1.0"), analysisResult.getSecurityMetrics().get("Rating"));
			
			for (MetricValue metricValue : analysisResult.getMainMetrics()) {
				if ("Lines of code".equals(metricValue.getName())) {
					Assert.assertEquals(new Double("26148.0"), metricValue.getValue());
					break;
				}
			}
			
			Object effort = analysisResult.getSecurityMetrics().get("Effort");
			Map<?, ?> effortMap = (Map<?, ?>) effort;
			
			// 313.45000000000005 instead of 313.45000000000007 due to rounding
			Assert.assertEquals(new Double("313.45000000000005"), effortMap.get("5Stars"));
			
			Assert.assertEquals(104, analysisResult.getInsightsData().getComponents());
			
			Map<String, Integer> obsRisk = analysisResult.getInsightsData().getRiskMap(InsightsData.OBSOLESCENCE_RISK);
			Assert.assertEquals(new Integer(15), obsRisk.get(InsightsData.HIGH));
			Assert.assertEquals(new Integer(77), obsRisk.get(InsightsData.MEDIUM));
			Assert.assertEquals(new Integer(1), obsRisk.get(InsightsData.LOW));
			Assert.assertEquals(new Integer(11), obsRisk.get(InsightsData.UNKNOWN));
			Assert.assertNull(obsRisk.get(InsightsData.NONE));
		}
	}
	
	@Test
	public void testReadAnalysisResultDelivery() throws IOException {
		try (InputStream is = KiuwanUtilsTest.class.getResourceAsStream("output_delivery.json")) {
			AnalysisResult analysisResult = KiuwanUtils.readAnalysisResult(is);
			
			Assert.assertEquals("CRITICAL", analysisResult.getAnalysisBusinessValue());
			Assert.assertEquals(new Double("1.0"), analysisResult.getSecurityMetrics().get("Rating"));
			for (MetricValue metricValue : analysisResult.getMainMetrics()) {
				if ("Lines of code".equals(metricValue.getName())) {
					Assert.assertEquals(new Double("108490.0"), metricValue.getValue());
					break;
				}
			}			
			Object effort = analysisResult.getSecurityMetrics().get("Effort");
			Map<?, ?> effortMap = (Map<?, ?>) effort;
			
			// 313.45000000000005 instead of 313.45000000000007 due to rounding
			Assert.assertEquals(new Double("37.75"), effortMap.get("5Stars"));
			
			Assert.assertNull(analysisResult.getInsightsData());
			
			Assert.assertEquals(new Integer(365), analysisResult.getDeliveryFiles().getCount());
			Assert.assertEquals(new Integer(0), analysisResult.getDeliveryDefects().getNewDefects());
			Assert.assertEquals(new Integer(0), analysisResult.getDeliveryDefects().getRemovedDefects());
			Assert.assertEquals(new Integer(2114), analysisResult.getDeliveryDefects().getDefects());
			
			Assert.assertEquals(new Double("50.0"), analysisResult.getAuditResult().getApprovalThreshold());
			Assert.assertEquals("OK", analysisResult.getAuditResult().getOverallResult());
			Assert.assertEquals(new Double("100.0"), analysisResult.getAuditResult().getScore());
			Assert.assertEquals(new Integer(1), analysisResult.getAuditResult().getCheckpointResults().get(0).getWeight());
		}
	}
	
	private int testCmdCall(String originalString, String processedString) throws IOException, URISyntaxException {
		Process proc = new ProcessBuilder()
			.directory(resourcesPath.toFile())
			.command(windowsCmdFile, processedString)
			.redirectErrorStream(true)
			.start();

		try {
			String s;
			BufferedReader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			boolean found = false;
			while ((s = stdout.readLine()) != null) {
				System.out.println("Java output\t\t\t= " + s);
				if (originalString.equals(s)) {
					found = true;
					System.out.println("Match found for\t\t= " + originalString);
				}
			}
			Assert.assertTrue(found);

			int exitValue = proc.waitFor();
			return exitValue;

		} catch (InterruptedException e) {
			return -1;

		} finally {
			proc.getInputStream().close();
			proc.getOutputStream().close();
			proc.getErrorStream().close();
		}
	}
	
}
