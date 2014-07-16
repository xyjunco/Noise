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
import java.util.regex.Matcher;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.xobject.PDXObjectImage;
import org.apache.pdfbox.util.PDFTextStripper;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLTextExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;

class Paragraph {
	private String paragraph;
	Paragraph( String paragraph ) {
		this.paragraph = paragraph;
	}
}

class Article {
	private String name;
	private String topic;
	private String header;
	private List<Paragraph> li;
	private String footer;
	Article( String name, String topic, String header, List<Paragraph> li, String footer ) {
		this.name = name;
		this.topic = topic;
		this.header = header;
		this.li = li;
		this.footer = footer;
	}
}

public class Transform {
	
	
	//以回车符分regex，将正文进行段落分割
	public static String[] dividePara( String text )
	{
		String symbol = "\n";
		java.util.regex.Pattern p = java.util.regex.Pattern.compile(symbol);
		String[] graph = p.split(text);
		return graph;
	}
	
	//抽取doc格式中的正文及格式化处理
	public static String getDoc( InputStream is, String filename ) throws IOException
	{
		@SuppressWarnings("resource")
		//进行正文的抽取
		WordExtractor extractor = new WordExtractor(is);
		String[] graph = extractor.getParagraphText();
		List<Paragraph> li = new ArrayList<>();
		for( int i=1; i<graph.length; i++ ) {
			Paragraph obj = new Paragraph( graph[i] );
			li.add(obj);
		}
		@SuppressWarnings("deprecation")
		String xml = textToXml( filename, graph[0], extractor.getHeaderText(), li,  extractor.getFooterText() );
		return xml;
	}
	
	
	//处理docx格式的正文及其格式化处理
	public static String getDocx( String filename ) throws IOException, XmlException, OpenXML4JException
	{
		OPCPackage opc = POIXMLDocument.openPackage(filename);
		@SuppressWarnings("resource")
		POIXMLTextExtractor docx = new XWPFWordExtractor(opc);
		String text = docx.getText();

		List<Paragraph> li = new ArrayList<>();
		String[] para = dividePara(text);
		for( int i=1; i<para.length; i++ ) {
			Paragraph obj = new Paragraph( para[i] );
			li.add(obj);
		}
		String xml = textToXml( filename, para[0], null, li,  null );
		return xml;
	}
	
	//将文档进行格式化
	public static String textToXml( String filename, String topic, String header, List<Paragraph> li, String footer )
	{
		Article doc = new Article( filename, topic, header, li, footer );
		XStream xstream = new XStream();
		xstream.alias("ARTICLE", Article.class);
		xstream.alias("PARAGRAPH", Paragraph.class);
		return xstream.toXML(doc);
	}
	
	//对pdf格式文档进行抽取
	public static String getPdf( File file ) throws IOException 
	{
		PDDocument pdf = PDDocument.load(file);
		PDDocumentInformation document = pdf.getDocumentInformation();
		PDFTextStripper pts = new PDFTextStripper();
		getPicInPdf( pdf, file.toString() );
		String text = pts.getText(pdf);
		String[] para = dividePara(text);
		String title = document.getTitle();
		List<Paragraph> li = new ArrayList<>();
		for( String p : para )
		{
			Paragraph obj = new Paragraph(p);
			li.add(obj);
		}
		String xml = textToXml(file.toString(), title, null, li, null);
		return xml;
		
	}
	
	//抽取pdf格式文档中的图片 
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
	public static void getPicInDocx( File file ) throws IOException, XmlException, OpenXML4JException
	{
		String picName = file + ".jpg";
		OPCPackage opc = POIXMLDocument.openPackage(file.toString());
		XWPFDocument docx = new XWPFDocument(opc);
		List<?> picList = docx.getAllPictures();
		for( int i=0; i<picList.size(); i++ ) {
			XWPFPictureData pic = (XWPFPictureData) picList.get(i);
			@SuppressWarnings("resource")
			FileOutputStream fos = new FileOutputStream(picName);
			byte[] pic_data = pic.getData();
			fos.write(pic_data);
		}		
	}
	
	public static void main(String[] args) throws IOException, XmlException, OpenXML4JException, SAXException {
		@SuppressWarnings("resource")
		Scanner scanner = new Scanner(System.in);
		//System.out.println("Please input the path:");
		//System.out.println("Input \\\\ as delimiter in Windows, Such as D:\\\\document\\");
		String path = "/home/junco/java/";
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
						fw.write( getDoc( is, file[i].toString() ) );
					}
					//解析docx格式文件
					if( file[i].toString().endsWith(".docx"))
					{
						getPicInDocx(file[i]);
						fw.write(getDocx(file[i].toString()));	
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
		System.out.println("Done.");
	}
}
