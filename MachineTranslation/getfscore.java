import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Scanner;

public class getfscore {

	public static void main(String[] args) throws FileNotFoundException {
		String outputfile = args[0];
		String benchmarkfile = args[1];

		Scanner scanner1 = new Scanner(new BufferedReader(new FileReader(new File(outputfile))));
		Scanner scanner2 = new Scanner(new BufferedReader(new FileReader(new File(benchmarkfile))));

		ArrayList<String> translated = new ArrayList<String>();
		ArrayList<String> to_compare = new ArrayList<String>();

		int row_count = 0;
		while (scanner1.hasNextLine() && scanner2.hasNextLine()) {
			String line1 = scanner1.nextLine().trim();
			String line2 = scanner2.nextLine().trim();
			translated.add(row_count, line1);
			to_compare.add(row_count, line2);
			row_count++;
		}
		scanner1.close();
		scanner2.close();

		int count_getright = 0;
		int sum_all_translated = 0;
		int sum_all_to_compare = 0;
		for (int i = 0; i <= row_count - 1; i++) {
			String line_to_translate = translated.get(i);
			if (!line_to_translate.equals("")) {
				String[] temp = line_to_translate.split(" ");
				String line_to_compare = to_compare.get(i);
				sum_all_translated += temp.length;
				sum_all_to_compare += line_to_compare.split(" ").length;

				for (String word : temp) {
					if (line_to_compare.indexOf(word) != -1) {
						count_getright++;
					}
				}
			}
		}
		// get precision
		double precision = (count_getright + 0.0) / sum_all_translated;
		System.out.println("precision: " + precision);
		// get recall
		double recall = (count_getright + 0.0) / sum_all_to_compare;
		System.out.println("recall: " + recall);
		// F score
		System.out.println("F-score: " + 2 * (precision * recall) / (precision + recall));
	}
}
