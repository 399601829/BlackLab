package nl.inl.blacklab.testutil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;

import nl.inl.blacklab.search.Searcher;
import nl.inl.blacklab.search.Searcher.LuceneDocTask;
import nl.inl.util.FileUtil;

/** Export the original corpus from a BlackLab index. */
public class ExportCorpus {

	public static void main(String[] args) throws IOException {
		if (args.length != 2 && args.length != 3) {
			System.out.println("Usage: ExportCorpus <indexDir> <exportDir> [fromInputPath]");
			System.exit(1);
		}

		File indexDir = new File(args[0]);
		if (!indexDir.isDirectory() || !indexDir.canRead()) {
			System.out.println("Directory doesn't exist or is unreadable: " + indexDir);
			System.exit(1);
		}
		if (!Searcher.isIndex(indexDir)) {
			System.out.println("Not a BlackLab index: " + indexDir);
			System.exit(1);
		}

		File exportDir = new File(args[1]);
		if (!exportDir.isDirectory() || !exportDir.canWrite()) {
			System.out.println("Directory doesn't exist or cannot write to it: " + exportDir);
			System.exit(1);
		}
		
		String fromInputPath = null;
		if (args.length == 3) {
		    fromInputPath = args[2];
		}

		ExportCorpus exportCorpus = new ExportCorpus(indexDir);
		exportCorpus.export(exportDir, fromInputPath);
	}

	Searcher searcher;

	public ExportCorpus(File indexDir) throws IOException {
		searcher = Searcher.open(indexDir);
	}

	/** Export the whole corpus.
	 * @param exportDir directory to export to
	 */
	private void export(final File exportDir, final String fromInputPath) {

		final IndexReader reader = searcher.getIndexReader();

		searcher.forEachDocument(new LuceneDocTask() {

			int totalDocs = reader.maxDoc() - reader.numDeletedDocs();

			int docsDone = 0;

			@Override
			public void perform(Document doc) {
				String fromInputFile = doc.get("fromInputFile");
				if (fromInputPath == null || fromInputFile.startsWith(fromInputPath)) {
    				System.out.println(fromInputFile);
    				String xml = "";
    				boolean skip = false;
                    try {
                        xml = searcher.getContent(doc);
                    } catch (RuntimeException e) {
                        System.out.println("Exception exporting document " + fromInputFile + ", skipping");
                        skip = true;
                    }
                    if (!skip) {
        				File file = new File(exportDir, fromInputFile);
        				if (file.exists()) {
        					// Add a number so we don't have to overwrite the previous file.
        					file = FileUtil.addNumberToExistingFileName(file);
        				}
        				File dir = file.getParentFile();
        				if (!dir.exists())
        					dir.mkdirs(); // create any subdirectories required
        				try (PrintWriter pw = FileUtil.openForWriting(file)) {
        					pw.write(xml);
        				}
                    }
    				docsDone++;
    				if (docsDone % 100 == 0) {
    					int perc = docsDone * 100 / totalDocs;
    					System.out.println(docsDone + " docs exported (" + perc + "%)...");
    				}
				}
			}
		});
	}
}
