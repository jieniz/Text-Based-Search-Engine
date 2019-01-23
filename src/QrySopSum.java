
import java.io.*;

import retrieval_model.RetrievalModel;
import retrieval_model.RetrievalModelBM25;

/**
 * The SUM operator for BM25 retrieval models.
 */
public class QrySopSum extends QrySop {

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
		if (r instanceof RetrievalModelBM25) {
			return this.getScoreBM25(r);
		} else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the SUM operator.");
		}
	}
	
	/**
	 * get the default score for the indri retrieval model, same as the normal score.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @param docid The document id.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
		if (r instanceof RetrievalModelBM25) {
			return this.getScoreBM25(r);
		} else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the SUM operator.");
		}
	}

	/**
	 * get the score for the BM25 retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @param docid The document id.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	private double getScoreBM25(RetrievalModel r) throws IOException {
		double sum = 0.0;
		int docid = this.docIteratorGetMatch();

		for (int i = 0; i < this.args.size(); i++) {
			Qry q_i = this.args.get(i);
			if (q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docid) {
				sum += ((QrySop) this.args.get(i)).getScore(r);
			}
		}
		return sum;
	}

}
