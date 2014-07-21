import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.hwpf.usermodel.Table;
import org.apache.poi.hwpf.usermodel.TableCell;
import org.apache.poi.hwpf.usermodel.TableIterator;
import org.apache.poi.hwpf.usermodel.TableRow;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.ClassAliasingMapper;

class My_Table {
	@SuppressWarnings("unused")
	private List<String> table;
	My_Table( List<String> li ) {
		 table = li;
	}
}

public class Tabletest {
	public static String getTableInDoc( FileInputStream in  ) throws IOException {
	            POIFSFileSystem pfs = new POIFSFileSystem(in);     
	            HWPFDocument hwpf = new HWPFDocument(pfs);     
	            Range range = hwpf.getRange();//得到文档的读取范围  
	            TableIterator it = new TableIterator(range);  
	            String table = null;
	            //迭代文档中的表格  
	            while (it.hasNext())
	            {     
	        	    Table tb = (Table) it.next();     
			    ArrayList<String> li = new ArrayList<>();
	        	    //迭代行，默认从0开始  
	        	    for (int i = 0; i < tb.numRows(); i++) 
	        	    {     
	        		    TableRow tr = tb.getRow(i);
	        		    String row = "";
	        		    //迭代列，默认从0开始  
	        		    for (int j = 0; j < tr.numCells(); j++)
	        		    {     
	        			    TableCell td = tr.getCell(j);//取得单元格  
	        			    //取得单元格的内容  
	        			    for(int k=0;k<td.numParagraphs();k++)
	        			    {     
	        				    Paragraph para =td.getParagraph(k);     
	        				    String s = para.text() + ",";     
	        				    s = s.replaceAll("\u0007", "");
	        				    row += s;
	        			    }       
	        		    }  
	        		    li.add(row);
	        	    }   
	        	    table +=  format_xml(li);
	            } 
	            return table;
	}
	
	public static String getTableInDocx( XWPFDocument docx ) 
	{
		List<XWPFTable> tables = docx.getTables();
		List<XWPFTableRow> rows;
		List<XWPFTableCell> cells;
		String xml = null;
		
		for( XWPFTable table : tables )
		{
			ArrayList<String> li = new ArrayList<>();
			rows = table.getRows();
			for( XWPFTableRow row : rows )
			{
				String rowstring = "";
				cells = row.getTableCells();
				for( XWPFTableCell cell : cells )
				{
					rowstring += ( cell.getText() + "," );
				}
				li.add(rowstring);
			}
			xml += format_xml(li);
		}
		return xml;
	}
	
	public static String format_xml( List<String> li )
	{
		XStream xstream = new XStream();
    	    	My_Table t = new My_Table(li);
    	    	xstream.alias("TABLE", My_Table.class);
    	    	ClassAliasingMapper mapper = new ClassAliasingMapper(xstream.getMapper());
    	    	mapper.addClassAlias("row", String.class);
    	    	xstream.registerLocalConverter(My_Table.class, "table", new CollectionConverter(mapper));
    	    	String cur = xstream.toXML(t);
    	    	return cur;
	}
	
}

