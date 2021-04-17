class TestClass {
        
    public static void staticContext() {

        TestClass t = null;
        int x = 0;

        /*
         * VALID
         */

        // QualifiedRef 
        // x = t.pubfield;
        // x = t.privfield;
        // x = t.pubstatfield;
        x = t.privstatfield;
    }
        
       
    public int pubfield;
    private int privfield;
    public static int pubstatfield;
    private static int privstatfield;
}
