import java.util.*;
public class AmadeusTest
{
  public static void main(String[] args)
  {
    AmadeusLib lib = new AmadeusLib();
    try
    {
      List<Hotel> list = lib.callAmadeus(args[0], args[1]);
      for(int i = 0; i < list.size(); i++)
      {
        Hotel h = list.get(i);
        System.out.println("Name:" + h.name + ", id:" + h.id);
      }
    }
    catch(Exception e)
    {
    }
  }
}