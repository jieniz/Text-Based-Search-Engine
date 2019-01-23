
import java.io.IOException;

import retrieval_model.RetrievalModel;
import retrieval_model.RetrievalModelIndri;

/**
 * The WSUM operator for all retrieval models.
 */
public class QrySopWSum extends QrySop {

	/**
	 * Indicates whether the query has a match.
	 * 
	 * @param r The retrieval model that determines what is a match
	 * @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch(RetrievalModel r) {
		return this.docIteratorHasMatchMin(r);
	}

	/**
	 * Get a score for the document that docIteratorHasMatch matched.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getScore(RetrievalModel r) throws IOException {
		if (r instanceof RetrievalModelIndri) {
			return this.getScoreIndri(r);
		} else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the WSUM operator.");
		}
	}
	
	/**
	 * get the default score for the indri retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @param docid The document id.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
		int size = this.args.size();
		
		double weight = 0.0;
		for (int i = 0; i < size; i++) {
			weight += this.args.get(i).weight;
		}
		
		double score = 0.0;
		for (int i = 0; i < size; i++) {
			QrySop q_i = (QrySop) this.args.get(i);
			score += ((q_i.weight / weight) * q_i.getDefaultScore(r, docid));
		}
		return score;
	}

	/**
	 * get the score for the indri retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @param docid The document id.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getScoreIndri(RetrievalModel r) throws IOException {
		int size = this.args.size();
		
		double weight = 0.0;
		for (int i = 0; i < size; i++) {
			weight += this.args.get(i).weight;
		}
		
		double score = 0.0;
		int docid = this.docIteratorGetMatch();
		for (int i = 0; i < this.args.size(); i++) {
			QrySop q_i = (QrySop) this.args.get(i);
			if (q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docid) {
				score += ((q_i.weight / weight) * q_i.getScore(r));
			} else {
				score += ((q_i.weight / weight) * q_i.getDefaultScore(r, docid));
			}
		}
		return score;
	}

}
