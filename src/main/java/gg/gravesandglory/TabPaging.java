package gg.gravesandglory;

/** Prop menu tab paging (pure, so a standalone check can pin it). Up to SLOTS categories fill the tab row; with more, slot 0 and slot
 *  SLOTS-1 become previous/next arrows and the SLOTS-2 slots between them show one page of categories, so every category stays reachable. */
final class TabPaging {
    static final int SLOTS=9,PREV_SLOT=0,NEXT_SLOT=SLOTS-1;
    private TabPaging(){}
    static int tabsPerPage(int count){return count<=SLOTS?SLOTS:SLOTS-2;}
    static int pageCount(int count){return Math.max(1,(count+tabsPerPage(count)-1)/tabsPerPage(count));}
    static int pageOf(int index,int count){return index<0?0:index/tabsPerPage(count);}
    /** The category index shown in tab-row slot `slot` on `page`, or -1 for an arrow, an empty slot or a slot past the last category. */
    static int tabIndex(int slot,int count,int page){
        if(slot<0||slot>=SLOTS)return -1;
        int i=count<=SLOTS?slot:(slot==PREV_SLOT||slot==NEXT_SLOT?-1:page*tabsPerPage(count)+slot-1);
        return i>=0&&i<count?i:-1;
    }
    static boolean hasPrev(int count,int page){return count>SLOTS&&page>0;}
    static boolean hasNext(int count,int page){return count>SLOTS&&page<pageCount(count)-1;}
}
