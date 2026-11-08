package gg.gravesandglory;

import java.util.*;

public final class HandbookTextCheck {
    static int checks;
    static void check(boolean pass,String message){
        if(!pass)throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args){
        var worlds=List.of("lobby","rivergate","ironhold","frostmarch","hollowwood","kingsfield");
        var pages=HandbookText.pages(worlds);
        check(!pages.isEmpty()&&pages.size()<=HandbookText.MAX_PAGES,"1..100 pages: "+pages.size());
        for(int i=0;i<pages.size();i++){
            String p=pages.get(i);
            check(!p.isBlank(),"page "+(i+1)+" blank");
            check(p.length()<=HandbookText.PAGE_CHARS,"page "+(i+1)+" over 1023 chars");
            check(HandbookText.lines(p)<=HandbookText.PAGE_LINES,"page "+(i+1)+" has "+HandbookText.lines(p)+" lines: "+p);
            check(!p.contains("{worlds}"),"page "+(i+1)+" keeps the placeholder");
        }
        String all=String.join("\n",pages);
        for(String w:worlds)check(all.contains(w),"world missing: "+w);
        check(!all.contains("camp"),"camp canvas is gone");
        for(String cmd:List.of("/prop","/map","/warp","/handbook"))check(all.contains(cmd),"mentions "+cmd);
        // keys are tokens the book resolves, never a hardcoded default key
        check(HandbookText.KEY.matcher(all).find(),"keys are {key:...} tokens");
        check(!all.matches("(?s).*(Press [A-Z]\b|\b[A-Z] opens|[Rr]ight-click).*"),"no hardcoded key");
        check(HandbookText.lines("{key:key.use}")==1,"a key token is one short word");
        // the line estimate itself
        check(HandbookText.lines("")==1,"empty page is one line");
        check(HandbookText.lines("aaaaaaaaaaaaaaaaaaa")==1,"19 chars fit one line");
        check(HandbookText.lines("aaaaaaaaaaaaaaaaaaaa")==2,"20 chars wrap");
        check(HandbookText.lines("aaaa bbbb cccc dddd eeee")==2,"word wrap");
        check(HandbookText.lines("a\n\nb")==3,"newlines count");
        // a page that is too long must be caught
        check(HandbookText.lines("x ".repeat(200))>HandbookText.PAGE_LINES,"overflow detected");
        System.out.println("PASS "+checks+" handbook text checks");
    }
}
