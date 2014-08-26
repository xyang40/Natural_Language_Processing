import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class GSampler {

	public static void getTopicModels(File input) throws FileNotFoundException {

		HashMap<String, int[]> Tau_cnt = new HashMap<String, int[]>();
		// word : topic_id : cnt
		int[][] Sig_cnt = new int[1000][50];
		// doc_id : topic_id : cnt
		int[][] Doc_Asgn = new int[1000][];
		// doc_id : list of word and its assignment
		String[][] Doc_Word = new String[1000][];
		// doc_id : index and word

		int doc_id = -1;
		int word_id = -1;
		int[] asgn = null;
		String[] words = null;
		Scanner scanner = new Scanner(input);
		while (scanner.hasNext()) {
			String line = scanner.nextLine();

			if (line.length() == 0) {
				continue;
			}
			if (line.length() != 0 && line.charAt(0) != ' ') {
				doc_id++;
				if (doc_id > 0) {
					Doc_Asgn[doc_id - 1] = asgn;
					Doc_Word[doc_id - 1] = words;
				}

				word_id = 0;
				asgn = new int[Integer.parseInt(line)];
				words = new String[Integer.parseInt(line)];
				continue;
			}

			String[] temp = line.trim().split(" ");
			for (String word : temp) {
				int topic_asgn = new Random().nextInt(50);
				asgn[word_id] = topic_asgn;
				words[word_id] = word;

				if (Tau_cnt.containsKey(word)) {
					int[] tau_cnt = Tau_cnt.get(word);
					tau_cnt[topic_asgn]++;
					Tau_cnt.put(word, tau_cnt);
				} else {
					int[] tau_cnt = new int[50];
					tau_cnt[topic_asgn]++;
					Tau_cnt.put(word, tau_cnt);
				}

				Sig_cnt[doc_id][topic_asgn]++;

				word_id++;
			}
		}
		Doc_Asgn[999] = asgn;
		Doc_Word[999] = words;
		scanner.close();
		//double prev = 0;
		
        int iteration = 0;
		while (iteration<=9) {// while termination condition not reached
			double loglik = 0;
			int loc_doc_id = -1;
			int loc_word_id = 0;
			Scanner scan = new Scanner(input);
			while (scan.hasNext()) {
				String line = scan.nextLine();

				if (line.length() == 0) {
					continue;
				}
				if (line.length() != 0 && line.charAt(0) != ' ') {
					loc_doc_id++;
					loc_word_id = 0;
					continue;
				}

				String[] temp = line.trim().split(" ");
				for (String word : temp) {
					// get current topic asgn
					int loc_topic_id = Doc_Asgn[loc_doc_id][loc_word_id];
					// decre tau
					int[] tau_cnt = Tau_cnt.get(word);
					tau_cnt[loc_topic_id]--;
					Tau_cnt.put(word, tau_cnt);
					// decre sig
					Sig_cnt[loc_doc_id][loc_topic_id]--;

					// get prob
					// for Sig
					double[] sum_doc = new double[1000];
					for (int i = 0; i <= 999; i++) {
						for (int j = 0; j <= 49; j++) {
							sum_doc[i] += Sig_cnt[i][j];
						}
					}

					// resampling
					double sum = 0;
					for (double d : Sig_cnt[loc_doc_id]) {
						sum += (d + 0.5) / (sum_doc[loc_doc_id] + 50 * 0.5);
					}
					double random_topic = new Random().nextDouble();

					double[] range = new double[Sig_cnt[loc_doc_id].length];
					int new_topic_asgn = -1;
					for (int i = 0; i <= 49; i++) {
						if (i == 0) {
							range[i] = ((Sig_cnt[loc_doc_id][i] + 0.5) / (sum_doc[loc_doc_id] + 50 * 0.5)) / sum;
							if (random_topic >= 0 && random_topic < range[i]) {
								new_topic_asgn = i;
							}
						} else {
							range[i] = range[i - 1] + ((Sig_cnt[loc_doc_id][i] + 0.5) / (sum_doc[loc_doc_id] + 50 * 0.5)) / sum;
							if (random_topic >= range[i - 1] && random_topic <= range[i]) {
								new_topic_asgn = i;
							}
						}
					}

					// incre tau
					int[] tau_cnt_cur = Tau_cnt.get(word);
					tau_cnt_cur[new_topic_asgn]++;
					Tau_cnt.put(word, tau_cnt_cur);
					// incre sig
					Sig_cnt[loc_doc_id][new_topic_asgn]++;
					// Doc_Asgn
					Doc_Asgn[loc_doc_id][loc_word_id] = new_topic_asgn;

					loc_word_id++;
				}
			}
			scan.close();

			double[] sum_array = new double[50];
			for (Map.Entry<String, int[]> entry : Tau_cnt.entrySet()) {
				for (int i = 0; i <= 49; i++) {
					sum_array[i] += entry.getValue()[i];
				}
			}
			double[] sum_doc = new double[1000];
			for (int i = 0; i <= 999; i++) {
				for (int j = 0; j <= 49; j++) {
					sum_doc[i] += Sig_cnt[i][j];
				}
			}

			//
			// calculate likelihood
			for (int k = 0; k <= 1; k++) {
				// System.out.println(loglik);
				for (int i = 0; i <= Doc_Asgn[k].length - 1; i++) {
					String word = Doc_Word[k][i];
					double sum_word = 0;
					for (int j = 0; j <= 49; j++) {
						double sig_prob = (Sig_cnt[k][j] + 0.5) / (sum_doc[k] + 50 * 0.5);
						double tau_prob = (Tau_cnt.get(word)[j] + 0.5) / (sum_array[j] + Tau_cnt.size() * 0.5);
						sum_word += sig_prob * tau_prob;
					}
					loglik += Math.log(sum_word);
				}
			}

		
			if(iteration==9){
				System.out.println("Loglikelihood at the final iteration: "+loglik);
			}
			iteration++;
		}
		System.out.println(" ");
		
		//3522.2640
		// Article 17
		double sum_doc = 0.0;
		for (int j = 0; j <= 49; j++) {
			sum_doc += Sig_cnt[16][j];
		}
		System.out.println("probabilities for each topic in Article 17: ");
		for (int j = 0; j <= 49; j++) {
			System.out.println("topic: "+j+" p: "+(Sig_cnt[16][j]+ 0.5) / (sum_doc + 50 * 0.5));
		}
		
		//
		System.out.println("");
		System.out.println("Top 15 words for each topic: ");
		HashMap<String,Integer> word_sum = new HashMap<String,Integer>();
		for(Map.Entry<String,int[]> entry: Tau_cnt.entrySet()){
			int[] temp = entry.getValue();
			int sum = 0;
			for(int i:temp){
				sum+=i;
			}
			word_sum.put(entry.getKey(), sum);
		}
		double[][] cnt_prob = new double[50][15];
		String[][] cnt_word = new String[50][15];
		for(int topic=0;topic<=49;topic++){
			for(Map.Entry<String,int[]> entry: Tau_cnt.entrySet()){
				String word = entry.getKey();
				int[] topic_cnt = entry.getValue();
				double prob = (topic_cnt[topic]+5.0)/(word_sum.get(word)+5*50);
				double smallest_prob = 999;
				int smallest_index = 0;
				
				for(int i=0;i<=14;i++){
					if(cnt_prob[topic][i]<smallest_prob){
						smallest_prob = cnt_prob[topic][i];
						smallest_index = i;
					}
				}
				if(prob>=smallest_prob){
					cnt_prob[topic][smallest_index]=prob;
					cnt_word[topic][smallest_index]=word;
				}
			}
			System.out.print("topic "+topic+": ");
			for(String word:cnt_word[topic]){
				System.out.print(word+" ");
			}
			System.out.println(" ");
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		File input = new File(args[0]);
		//File input = new File("c://news1000.txt");
		getTopicModels(input);
	}

}
