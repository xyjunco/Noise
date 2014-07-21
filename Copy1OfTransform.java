import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.xmlbeans.XmlException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.mapper.ClassAliasingMapper;

class Article {
	@SuppressWarnings("unused")
	private String name;
	@SuppressWarnings("unused")
	private String header;
	@SuppressWarnings("unused")
	private List<String> li;
	@SuppressWarnings("unused")
	private String footer;
	Article( String name, String header, List<String> li, String footer ) {
		this.name = name;
		this.header = header;
		this.li = li;
		this.footer = footer;
	}
}

public class Copy1OfTransform {
	
	//抽取doc格式中的正文及格式化处理
	public static String getDoc( InputStream is, File file ) throws IOException
	{
		@SuppressWarnings("resource")
		WordExtractor extractor = new WordExtractor(is);
		FileInputStream f = new FileInputStream(file);
		//抽取doc文档中的表格
		String table = Tabletest.getTableInDoc(f);
		//抽取正文
		String[] graph = extractor.getParagraphText();
		List<String> li = new ArrayList<>();
		for( int i=0; i<graph.length; i++ ) {
			graph[i] = graph[i].replaceAll("\r\n", "");
			graph[i] = graph[i].replaceAll("\u0007", "");
			li.add(graph[i]);
		}
		@SuppressWarnings("deprecation")
		String xml = textToXml( file.getName(), extractor.getHeaderText(), li,  extractor.getFooterText() );
		return xml + table;
	}
	
	//处理docx格式的正文及其格式化处理
	public static String getDocx( File file ) throws IOException, XmlException, OpenXML4JException
	{
		FileInputStream fin = new FileInputStream(file);
		XWPFDocument docx = new XWPFDocument(fin);
		List<XWPFHeader> header = docx.getHeaderList();
		List<XWPFFooter> footer = docx.getFooterList();
		List<XWPFParagraph> paras = docx.getParagraphs();
		List<String> li = new ArrayList<>();
		
		XWPFHeader head = header.get(0);
		XWPFFooter foot = footer.get(0);
		for( XWPFParagraph para : paras )
		{
			li.add( para.getText() );
		}
		String xml = textToXml( file.getName(), head.getText(), li, foot.getText() );
		xml += Tabletest.getTableInDocx(docx);
		getPicInDocx( docx, file.toString() + "jpg" );

		return xml;
	}
	
	//将文档进行格式化
	public static String textToXml( String filename, String header, List<String> li, String footer )
	{
		//初始化Article对象
		Article doc = new Article( filename, header, li, footer );
		XStream xstream = new XStream();
		xstream.alias("artical", Article.class);
		xstream.aliasField("text", Article.class, "li");
		ClassAliasingMapper mapper = new ClassAliasingMapper(xstream.getMapper());
		mapper.addClassAlias("para", String.class);
		xstream.registerLocalConverter(Article.class, "li", new CollectionConverter(mapper));
		return xstream.toXML(doc);
	}
	
	//对pdf格式文档进行抽取
	public static String getPdf( File file ) throws IOException 
	{
		PDDocument pdf = PDDocument.load(file);
		//PDDocumentInformation document = pdf.getDocumentInformation();
		PDFTextStripper pts = new PDFTextStripper();
		getPicInPdf( pdf, file.toString() );
		String text = pts.getText(pdf);
		String[] para = dividePara(text);
		List<String> li = new ArrayList<>();
		for( String p : para )
		{
			li.add(p);
		}
		String xml = textToXml(file.getName(), null, li, null);
		pdf.close();
		return xml;
	}
	
	//以回车符分regex，将正文进行段落分割
	public static String[] dividePara( String text )
	{
		String symbol = "\n";
		java.util.regex.Pattern p = java.util.regex.Pattern.compile(symbol);
		String[] graph = p.split(text);
		return graph;
	}
	
	//抽取pdf格式文档中的图片 
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <E> void getPicInPdf( PDDocument document, String filename ) throws IOException
	{
		PDDocumentCatalog cata = document.getDocumentCatalog();
		List pages = cata.getAllPages();
		for( int i=0; i<pages.size(); i++ )
		{
			PDPage page = (PDPage)pages.get(i);
			if( page != null )
			{
				PDResources res = page.findResources();
				@SuppressWarnings("deprecation")
				Map imgs = res.getImages();
				if( imgs != null )
				{
					Set<E> keyset = imgs.keySet();
					Iterator it = keyset.iterator();
					while( it.hasNext() )
					{
						Object obj = it.next();
						PDXObjectImage img = (PDXObjectImage) imgs.get(obj);
						img.write2file( filename );
					}
				}
			}
		}
	}
	
	//抽取doc格式文件中的图片
	public static void getPicInDoc( File file ) throws IOException
	{
		String picName = file + ".jpg";
		FileInputStream pic = new FileInputStream(file);
		HWPFDocument doc = new HWPFDocument(pic);
		List<?> picList = doc.getPicturesTable().getAllPictures();
		Picture picture = (Picture)picList.get(0);
		try {
			picture.writeImageContent(new FileOutputStream(picName));
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	//抽取docx格式文件中的图片
	public static void getPicInDocx( XWPFDocument docx, String picName ) throws IOException, XmlException, OpenXML4JException
	{
		List<?> picList = docx.getAllPictures();
		for( int i=0; i<picList.size(); i++ ) {
			XWPFPictureData pic = (XWPFPictureData) picList.get(i);
			@SuppressWarnings("resource")
			FileOutputStream fos = new FileOutputStream(picName);
			byte[] pic_data = pic.getData();
			fos.write(pic_data);
		}		
	}
	
	public static void analyse( String path ) throws IOException, XmlException, OpenXML4JException
	{
		File dir = new File(path);
		File[] file = dir.listFiles();
		for( int i=0; i<file.length; i++ )
		{
			if( !file[i].isDirectory() )
			{
				//解析doc,docx格式文件
				if( file[i].toString().matches(".*\\.docx?"))
				{
					String[] tem = file[i].toString().split("\\.");
					FileWriter fw = new FileWriter(tem[0]+".txt");
					//解析doc格式文件
					if( file[i].toString().endsWith(".doc"))
					{
						getPicInDoc(file[i]);
						InputStream is = new FileInputStream(file[i].toString());
						String text = getDoc( is, file[i] );
						fw.write(  text );
					}
					//解析docx格式文件
					if( file[i].toString().endsWith(".docx"))
					{
						fw.write(getDocx(file[i]));	
					}
					fw.close();
				}
				if( file[i].toString().matches(".*\\.pdf"))
				{
					String[] tem = file[i].toString().split("\\.");
					FileWriter fw = new FileWriter(tem[0]+".txt");
					String text = getPdf(file[i]);
					fw.write(text);
					fw.close();
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException, XmlException, OpenXML4JException{
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		System.out.println("Please input the path:");
		System.out.println("Input \\\\ as delimiter in Windows, Such as D:\\\\document\\");
		String path = scanner.nextLine();
		try {
			analyse(path);	
		} catch( FileNotFoundException f ) {
			f.printStackTrace();
		} catch( NullPointerException n ) {
			System.out.println("Path incorrect. Please confirm and input again.");
			System.exit(0);
		}
		
		System.out.println("Done.");
	}
}
