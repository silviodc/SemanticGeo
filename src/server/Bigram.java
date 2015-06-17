package server;



import java.util.ArrayList;
import java.util.List;

public class Bigram {
	
	/* Generates number between 0.00 and 1.00 */
	public double stringSimilarityScore(List<char[]> bigram1, List<char[]> bigram2) {
	    List<char[]> copy = new ArrayList<char[]>(bigram2);
	    int matches = 0;
	    for (int i = bigram1.size(); --i >= 0;) {
	        char[] bigram = bigram1.get(i);
	        for (int j = copy.size(); --j >= 0;) {
	            char[] toMatch = copy.get(j);
	            if (bigram[0] == toMatch[0] && bigram[1] == toMatch[1]) {
	                copy.remove(j);
	                matches += 2;
	                break;
	            }
	        }
	    }
//   System.out.println( (double) matches / (bigram1.size() + bigram2.size()));
	    return (double) matches / (bigram1.size() + bigram2.size());
	}
	
	public List<char[]> bigram(String input) {
	    ArrayList<char[]> bigram = new ArrayList<char[]>();
	    for (int i = 0; i < input.length() - 1; i++) {
	        char[] chars = new char[2];
	        chars[0] = input.charAt(i);
	        chars[1] = input.charAt(i+1);
	        bigram.add(chars);
	    }
	    return bigram;
	}
}
