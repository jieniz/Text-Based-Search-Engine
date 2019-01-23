
import java.io.*;

import retrieval_model.RetrievalModel;
import retrieval_model.RetrievalModelIndri;
import retrieval_model.RetrievalModelRankedBoolean;
import retrieval_model.RetrievalModelUnrankedBoolean;

/**
 * The AND operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

	/**
	 * Indicates whether the query has a match.
	 * 
	 * @param r The retrieval model that determines what is a match
	 * @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch(RetrievalModel r) {
		if (r instanceof RetrievalModelIndri) {
			// the indri model uses docIteratorHasMatchMin and others use all
            return this.docIteratorHasMatchMin(r);
        }
        return this.docIteratorHasMatchAll(r);
	}

	/**
	 * Get a score for the document that docIteratorHasMatch matched.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getScore(RetrievalModel r) throws IOException {
		if (r instanceof RetrievalModelUnrankedBoolean) {
			return this.getScoreUnrankedBoolean(r);
		} else if (r instanceof RetrievalModelRankedBoolean) {
			return this.getScoreRankedBoolean(r);
		} else if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri(r);
        } else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the AND operator.");
		}
	}

	/**
	 * getScore for the UnrankedBoolean retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	private double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return 1.0;
		}
	}

	/**
	 * getScore for the RankedBoolean retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	private double getScoreRankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			double minScore = Double.MAX_VALUE;
	        for (int i = 0; i < this.args.size(); i++) {
	            double score = ((QrySop) this.args.get(i)).getScore(r);
	            minScore = minScore > score ? score : minScore;
	        }
	        return minScore;
		}
	}
	
	/**
	 * get the default score for the Indri retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @param docid the id of the document
	 * @return The document score
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
        double score = 1.0;
        for (int i = 0; i < this.args.size(); i ++) {
            score *= ((QrySop) this.args.get(i)).getDefaultScore(r, docid);
        }
        return Math.pow(score, 1.0 / this.args.size());
    }

	/**
	 * get the score for the Indri retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @param docid the id of the document
	 * @return The document score
	 * @throws IOException Error accessing the Lucene index
	 */
	private double getScoreIndri(RetrievalModel r) throws IOException {
        double score = 1.0;
        int docid = this.docIteratorGetMatch();
        int size = this.args.size();
        
        for (int i = 0; i < size; i ++) {
            QrySop q_i = ((QrySop) this.args.get(i));
            if (q_i.docIteratorHasMatch(r) && q_i.docIteratorGetMatch() == docid){
            	score *= q_i.getScore(r);
            }else {
            	score *= q_i.getDefaultScore(r, docid);
            }
        }
        return Math.pow(score, 1.0 / size);
    }

}
