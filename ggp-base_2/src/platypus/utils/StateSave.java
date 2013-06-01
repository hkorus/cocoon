package platypus.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;



public class StateSave {
	private static final String DATE_FORMAT = "yyyyMMddHHmmssSSS";
	private static final String DIR_PREFIX = "saved_states/";
	private static String getCurrentTime(){
		return (new SimpleDateFormat(DATE_FORMAT).format(new Date()));
	}
	
	public static String save(Object objectToSave){
		String objClass = objectToSave.getClass().getName();
		return save(objectToSave, objClass);
	}

	public static String save(Object objectToSave, String prefix){
		String filename = DIR_PREFIX + prefix + getCurrentTime() +".platypus";
		ObjectOutputStream out;

		try{
			File file = new File(filename);
			out = new ObjectOutputStream(new FileOutputStream(file));
			out.writeObject(objectToSave);


			out.flush();
			out.close();
			System.out.println("STATE SAVE: Object written to file: " + filename);
			return filename;
		}
		catch (FileNotFoundException ex) {
			System.out.println("Error with specified file") ;
			ex.printStackTrace();
		}
		catch (IOException ex) {
			System.out.println("Error with I/O processes") ;
			ex.printStackTrace();
		}
		return null;
	}

	public static Object load(String filename){
		
		try{

			FileInputStream fis = new FileInputStream(DIR_PREFIX + filename);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Object obj = ois.readObject();
			ois.close();
			return obj;
		}
		catch (FileNotFoundException e){

			e.printStackTrace();
		}
		catch (IOException e){

			e.printStackTrace();
		}
		catch (ClassNotFoundException e){

			e.printStackTrace();
		}
		return null;
	}
}
