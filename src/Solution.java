import org.dom4j.DocumentException;

public class Solution {

    public static void main(String[] args) throws PLDLParsingException, DocumentException, PLDLAnalysisException, REParsingException {
        new Thread() {
            @Override
            public void run() {
                try {
                    Graphics.main();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }.start();

//        PreParse parser = new PreParse("TEST.pldl", null);
//        CFG cfg = parser.getCFG();
//        cfg.augmentCFG();
//        TransformTable table = cfg.getTable();
//        System.out.println(table);
//        AnalysisTree tree = table.getAnalysisTree(parser.getSymbols("12+ 34 * (5+62)", cfg));
//        System.out.println(tree);
//        System.out.println(PLDLParsingWarning.getLoggings());
        
        RE re = new SimpleREApply("a|e");
        NFA nfa = re.getNFA();
        //nfa.draw();
        DFA dfa = nfa.toDFA();
        dfa.simplify();
        dfa.draw();
    }
}