// renamecpp.cpp : 定义控制台应用程序的入口点。
//


#include "stdafx.h"
#include <windows.h>
#include <assert.h>


typedef BOOL (WINAPI *EnumerateFunc) (LPCSTR lpFileOrPath, void* pUserData);

void doFileEnumeration(LPSTR lpPath, BOOL bRecursion, BOOL bEnumFiles, EnumerateFunc pFunc, void* pUserData)
{

 static BOOL s_bUserBreak = FALSE;
 try{
  //-------------------------------------------------------------------------
  if(s_bUserBreak) return;
  
  int len = strlen(lpPath);
  if(lpPath==NULL || len<=0) return;
  
  //NotifySys(NRS_DO_EVENTS, 0,0);
  
  char path[MAX_PATH];
  strcpy(path, lpPath);
  if(lpPath[len-1] != '\\') strcat(path, "\\");
  strcat(path, "*");
  
  WIN32_FIND_DATA fd;
  HANDLE hFindFile = FindFirstFile(path, &fd);
  if(hFindFile == INVALID_HANDLE_VALUE)
  {
   ::FindClose(hFindFile); return;
  }
  
  char tempPath[MAX_PATH]; BOOL bUserReture=TRUE; BOOL bIsDirectory;
  
  BOOL bFinish = FALSE;
  while(!bFinish)
  {
   strcpy(tempPath, lpPath);
   if(lpPath[len-1] != '\\') strcat(tempPath, "\\");
   strcat(tempPath, fd.cFileName);
   
   bIsDirectory = ((fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) != 0);
   
   //如果是.或..
   if( bIsDirectory
    && (strcmp(fd.cFileName, ".")==0 || strcmp(fd.cFileName, "..")==0)) 
   {  
    bFinish = (FindNextFile(hFindFile, &fd) == FALSE);
    continue;
   }
   
   if(pFunc && bEnumFiles!=bIsDirectory)
   {
    bUserReture = pFunc(tempPath, pUserData);
    if(bUserReture==FALSE)
    {
     s_bUserBreak = TRUE; ::FindClose(hFindFile); return;
    }
   }
   
   //NotifySys(NRS_DO_EVENTS, 0,0);
   
   if(bIsDirectory && bRecursion) //是子目录
   {
    doFileEnumeration(tempPath, bRecursion, bEnumFiles, pFunc, pUserData);
   }
   
   bFinish = (FindNextFile(hFindFile, &fd) == FALSE);
  }
  
  ::FindClose(hFindFile);
  
  //-------------------------------------------------------------------------
 }catch(...){ assert(0); return; }
}
BOOL WINAPI myEnumerateFunc(LPCSTR lpFileOrPath, void* pUserData)
{


 //printf("%s\n", lpFileOrPath);
 char newbuf[1024];
 memset(newbuf,0,1024);
 const char *p = lpFileOrPath;
 int i = 0; 
 
 while (*++p != '\0' && *p != '.')
 {
  if (*p == '_' && ++i > 3) {
  
   break;
  }

 }
 int len = p - lpFileOrPath;
 memcpy(newbuf,lpFileOrPath,len);
 strcat(newbuf, ".csv");
 //printf("newpath: %s\n",newbuf);
 
 if(!rename(lpFileOrPath, newbuf))
{
 printf("%s成功重命名为%s\n", lpFileOrPath, newbuf);
}
 //if((pdot = strrchr(lpFileOrPath, '.')) && stricmp(pdot, ".mp3") == 0)
 //{
  //printf("%s\n", lpFileOrPath);
 //}
 return TRUE;
}

int main()
{
 doFileEnumeration("d:\\data", TRUE, TRUE, myEnumerateFunc, NULL);
 return 0;
}

