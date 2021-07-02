package tools;

public class Parameters {
	
	private Parameters() {}
	
	// Projects Names
	public static final String BOOKKEEPER = "BOOKKEEPER";
	public static final String SECONDOPROG = "FALEXI";
	
	// Projects settings
	public static final String GIT_PROJ_ORG = "apache";
	public static final String GIT_PROJ_NAME = "bookkeeper";
	public static final String JIRA_PROJ_NAME = "BOOKKEEPER";
	public static final String REST_API = "https://issues.apache.org/jira/rest/api/2/project/";
	
	// Strings settings
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String TAG_FORMAT = "/ref/tags/";
	public static final String FILTER_REL_NAME = "release-";
	public static final String FILTER_FILE_TYPE = ".java";
	public static final String RELEASED_JSON = "released";
	public static final String NAME_JSON = "name";
	public static final String INCREMENTAL = "incremental";
	public static final String MOVING_WINDOW = "moving_window";
	
	// Diffentries types
	public static final String MODIFY = "MODIFY";
	public static final String ADD = "ADD";
	public static final String DELETE = "DELETE";
	public static final String RENAME = "RENAME";
	
	// Errors
	public static final String CSV_ERROR = "Error in csv writer";
	
	// Paths
	public static final String OUTPUT_PATH = "output\\";
	public static final String DATASET_CSV = "_Dataset.csv";
	public static final String DATASET_ARFF = "_Dataset.arff";
	public static final String WEKA_CSV = "_TempDataset.csv";
	public static final String RESULT_CSV = "_AnalysisResult.csv";
}
