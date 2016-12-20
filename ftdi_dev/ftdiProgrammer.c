/*!
 * \file sample-dynamic.c
 *
 * \author FTDI
 * \date 20110512
 *
 * Copyright © 2000-2014 Future Technology Devices International Limited
 *
 *
 * THIS SOFTWARE IS PROVIDED BY FUTURE TECHNOLOGY DEVICES INTERNATIONAL LIMITED ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FUTURE TECHNOLOGY DEVICES INTERNATIONAL LIMITED
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Project: libMPSSE
 * Module: SPI Sample Application - Interfacing 94LC56B SPI EEPROM
 *
 * Rivision History:
 * 0.1  - 20110512 - Initial version
 * 0.2  - 20110801 - Changed LatencyTimer to 255
 * 					 Attempt to open channel only if available
 *					 Added & modified macros
 *					 Included stdlib.h
 * 0.3  - 20111212 - Added comments
 * 0.41 - 20140903 - Fixed compilation warnings
 *					 Added testing of SPI_ReadWrite()
 */

/******************************************************************************/
/* 							 Include files										   */
/******************************************************************************/
/* Standard C libraries */
#include<stdio.h>
#include<stdlib.h>
#include<string.h>
/* OS specific libraries */
#ifdef _WIN32
#include<windows.h>
#endif

/* Include D2XX header*/
#include "ftd2xx.h"

/* Include libMPSSE header */
#include "libMPSSE_spi.h"

/******************************************************************************/
/*								Macro and type defines							   */
/******************************************************************************/
/* Helper macros */
#ifdef _WIN32
	#define GET_FUN_POINTER	GetProcAddress
	#define CHECK_ERROR(exp) {if(exp==NULL){printf("%s:%d:%s():  NULL \
expression encountered \n",__FILE__, __LINE__, __FUNCTION__);exit(1);}else{;}};
#endif

#define APP_CHECK_STATUS(exp) {if(exp!=FT_OK){printf("%s:%d:%s(): status(0x%x) \
!= FT_OK\n",__FILE__, __LINE__, __FUNCTION__,exp);exit(1);}else{;}};
#define CHECK_NULL(exp){if(exp==NULL){printf("%s:%d:%s():  NULL expression \
encountered \n",__FILE__, __LINE__, __FUNCTION__);exit(1);}else{;}};

/* Application specific macro definations */
#define SPI_DEVICE_BUFFER_SIZE		256
#define SPI_WRITE_COMPLETION_RETRY		10
#define START_ADDRESS_EEPROM 	0x00 /*read/write start address inside the EEPROM*/
#define END_ADDRESS_EEPROM		0x10
#define RETRY_COUNT_EEPROM		10	/* number of retries if read/write fails */
#define CHANNEL_TO_OPEN			0	/*0 for first available channel, 1 for next... */
#define SPI_SLAVE_0				0
#define SPI_SLAVE_1				1
#define SPI_SLAVE_2				2
#define DATA_OFFSET				4
#define USE_WRITEREAD			1
#define GETBIT1(x)              ( x & 0x01)

	
//prototypes
char *getSubstring(char*, int, int);
int getSize(char*);
int hasPrefix(char*, char*);
int hasSuffix(char*, char*);    
/******************************************************************************/
/*								Global variables							  	    */
/******************************************************************************/
static FT_HANDLE ftHandle;
static uint8 buffer[SPI_DEVICE_BUFFER_SIZE] = {0};

/******************************************************************************/
/*						Public function definitions						  		   */
/******************************************************************************/
/*!
 * \brief Writes to EEPROM
 *
 * This function writes a byte to a specified address within the 93LC56B EEPROM
 *
 * \param[in] slaveAddress Address of the I2C slave (EEPROM)
 * \param[in] registerAddress Address of the memory location inside the slave to where the byte
 *			is to be written
 * \param[in] data The byte that is to be written
 * \return Returns status code of type FT_STATUS(see D2XX Programmer's Guide)
 * \sa Datasheet of 93LC56B http://ww1.microchip.com/downloads/en/DeviceDoc/21794F.pdf
 * \note
 * \warning
 */
 
 
FTDI_API void print_data( unsigned char *buf, unsigned int size)
{
    unsigned int i;
    for( i = 0;i < size; i++)
    {
        //fprintf( stderr, "%02X ", buf[i]);
        fprintf( stdout, "%02X ", buf[i]);
        if( (i+1) % 16 == 0)
        {
            fprintf( stdout, "\n");
        }
    }
    if( i % 16 != 0)
        fprintf( stdout, "\n");
}

FTDI_API int str_2_hex(char* buf_in, unsigned char* buf_out)
{
	const char *delim = " ";
	char * p;
	char tmp[3];
	int val;
	int size = 0;
	size_t len,i;
	int rw = -1;
	sscanf(strtok(buf_in,delim),"%x",&val);
	buf_out[size] = val;
	size++;
	while((p = strtok(NULL,delim)))
	{
		if (strlen(p) == 2) 
		{
			//fprintf(stderr,"p=%s",p);
			sscanf(p,"%x",&val);
			if(size == 2)
			{
				//fprintf(stderr,"buf2=%x\n",GETBIT1(val));
				if (GETBIT1(val) == 1)
				{
					rw = 1;
				}
				else
				{
					rw = 0;
				}
			}
			memset(tmp,0,sizeof(tmp));
			strncpy(tmp,p,2);

			if (strcmp(tmp,"xx") == 0 || strcmp(tmp,"XX") == 0)
			{
				if(rw == 1)
				{
					buf_out[size++]=0x00;
				}
				else if(rw == 0)
				{
					fprintf(stderr,"error in data file, should not contain xx\n");
				}
				else
				{
					buf_out[size++] = val;
				}

			}
			else
			{
               // fprintf(stderr,"tmp=%s,",tmp);
				sscanf(tmp,"%x",&val);
				buf_out[size]=val;
				size++;	
			}
			//buf_out[size++] = val;
		}
		else
		{
			len=strlen(p);
			for (i=0;i < len-1; i+=2) 
			{
				memset(tmp,0,sizeof(tmp));
				strncpy(tmp,p,2);
				if (strcmp(tmp,"xx"))
				{
					//fprintf(stderr,"tmp=%s,",tmp);
					sscanf(tmp,"%x",&val);
					buf_out[size++]=val;
				}
				else
				{
					if(rw == 1)
					{
						buf_out[size++]=0x00;
					}
					else
					{
						fprintf(stderr,"error in data file, should not contain xx\n");
					}
				}
				p+=2;
			}
		}
	}
	return size;
}


char *getSeparator() 
{
#ifdef _WIN32
    return "\\output.txt";
#else  
    return "/output.txt";
#endif
}


 
int getSize(char *str)
{
    int size = strlen(str);
    if(str[size - 1] == '\n')
        return size - 1;
    else 
        return size;
}

char *getSubstring(char *str, int pos, int size)
{
    char *tmp = (char *) malloc(sizeof(char) * (size + 1));
    memcpy(tmp, &str[pos], size);
    tmp[size] = '\0';
    return tmp;
}

int hasPrefix(char *str, char *prefix)
{
    int size1 = strlen(prefix);
    int prefixPos = 0;
    
    char *subStr = getSubstring(str, prefixPos, size1);
    if(strcmp(subStr, prefix) == 0)
        return 0;
    else
        return 1;
    
}

int hasSuffix(char *str,char *suffix)
{
    //int size1 = strlen(str) - 1; //fgets put an extra character (new line)
    int size1 = getSize(str);
    int size2 = strlen(suffix);
    int suffixPos = size1 - size2;
    
    char *subStr = getSubstring(str, suffixPos, size2);
    //printf("String = %s, Sub = %s, Suffix = %s\n", str, subStr, suffix);
    if(strcmp(subStr, suffix) == 0)
        return 0;
    else
        return 1;
    
} 
 
static FT_STATUS read_byte(uint8 slaveAddress, uint8 address, uint16 *data)
{
	uint32 sizeToTransfer = 0;
	uint32 sizeTransfered;
	uint8 writeComplete=0;
	uint32 retry=0;
	FT_STATUS status;

	/* CS_High + Write command + Address */
	sizeToTransfer=1;
	sizeTransfered=0;
	buffer[0] = 0xC0;/* Write command (3bits)*/
	buffer[0] = buffer[0] | ( ( address >> 3) & 0x0F );/*5 most significant add bits*/
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE);
	APP_CHECK_STATUS(status);

	/*Write partial address bits */
	sizeToTransfer=4;
	sizeTransfered=0;
	buffer[0] = ( address & 0x07 ) << 5; /* least significant 3 address bits */
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BITS);
	APP_CHECK_STATUS(status);

	/*Read 2 bytes*/
	sizeToTransfer=2;
	sizeTransfered=0;
	status = SPI_Read(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE);
	APP_CHECK_STATUS(status);

	*data = (uint16)(buffer[1]<<8);
	*data = (*data & 0xFF00) | (0x00FF & (uint16)buffer[0]);

	return status;
}

/*!
 * \brief Reads from EEPROM
 *
 * This function reads a byte from a specified address within the 93LC56B EEPROM
 *
 * \param[in] slaveAddress Address of the I2C slave (EEPROM)
 * \param[in] registerAddress Address of the memory location inside the slave from where the
 *			byte is to be read
 * \param[in] *data Address to where the byte is to be read
 * \return Returns status code of type FT_STATUS(see D2XX Programmer's Guide)
 * \sa Datasheet of 93LC56B http://ww1.microchip.com/downloads/en/DeviceDoc/21794F.pdf
 * \note
 * \warning
 */
static FT_STATUS write_byte(uint8 slaveAddress, uint8 address, uint16 data)
{
	uint32 sizeToTransfer = 0;
	uint32 sizeTransfered=0;
	uint8 writeComplete=0;
	uint32 retry=0;
	FT_STATUS status;

	/* Write command EWEN(with CS_High -> CS_Low) */
	sizeToTransfer=11;
	sizeTransfered=0;
	buffer[0]=0x9F;/* SPI_EWEN -> binary 10011xxxxxx (11bits) */
	buffer[1]=0xFF;
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BITS|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE);
	APP_CHECK_STATUS(status);

	/* CS_High + Write command + Address */
	sizeToTransfer=1;
	sizeTransfered=0;
	buffer[0] = 0xA0;/* Write command (3bits) */
	buffer[0] = buffer[0] | ( ( address >> 3) & 0x0F );/*5 most significant add bits*/
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE);
	APP_CHECK_STATUS(status);

	/*Write 3 least sig address bits */
	sizeToTransfer=3;
	sizeTransfered=0;
	buffer[0] = ( address & 0x07 ) << 5; /* least significant 3 address bits */
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BITS);
	APP_CHECK_STATUS(status);

	/* Write 2 byte data + CS_Low */
	sizeToTransfer=2;
	sizeTransfered=0;
	buffer[0] = (uint8)(data & 0xFF);
	buffer[1] = (uint8)((data & 0xFF00)>>8);
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE);
	APP_CHECK_STATUS(status);

	/* Wait until D0 is high */
#if 1
	/* Strobe Chip Select */
	sizeToTransfer=0;
	sizeTransfered=0;
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BITS|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE);
	APP_CHECK_STATUS(status);
#ifndef __linux__
	Sleep(10);
#endif
	sizeToTransfer=0;
	sizeTransfered=0;
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BITS|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE);
	APP_CHECK_STATUS(status);
#else
	retry=0;
	state=FALSE;
	SPI_IsBusy(ftHandle,&state);
	while((FALSE==state) && (retry<SPI_WRITE_COMPLETION_RETRY))
	{
		printf("SPI device is busy(%u)\n",(unsigned)retry);
		SPI_IsBusy(ftHandle,&state);
		retry++;
	}
#endif
	/* Write command EWEN(with CS_High -> CS_Low) */
	sizeToTransfer=11;
	sizeTransfered=0;
	buffer[0]=0x8F;/* SPI_EWEN -> binary 10011xxxxxx (11bits) */
	buffer[1]=0xFF;
	status = SPI_Write(ftHandle, buffer, sizeToTransfer, &sizeTransfered,
		SPI_TRANSFER_OPTIONS_SIZE_IN_BITS|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE|
		SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE);
	APP_CHECK_STATUS(status);
	return status;
}

/*!
 * \brief Main function / Entry point to the sample application
 *
 * This function is the entry point to the sample application. It opens the channel, writes to the
 * EEPROM and reads back.
 *
 * \param[in] none
 * \return Returns 0 for success
 * \sa
 * \note
 * \warning
 */
int main(int argc, char **argv)
{

    if(argc != 3) 
    {
        fprintf(stderr, "Usage %s [inputFile] [output directory location]", argv[0]);
        return 1;
    }
    
    char outputFile[1024];
    strcpy (outputFile, argv[2]);
    strcat (outputFile, getSeparator());
    
	FT_STATUS status = FT_OK;
	FT_DEVICE_LIST_INFO_NODE devList = {0};
	ChannelConfig channelConf = {0};
	uint8 address = 0;
	uint32 channels = 0;
	uint16 data = 0;
	uint8 i = 0;
	uint8 latency = 255;
    uint32 clkRate = 1;
	
	//channelConf.ClockRate = 5000;
	channelConf.ClockRate = clkRate * 1000;
	channelConf.LatencyTimer = latency;
	channelConf.configOptions = SPI_CONFIG_OPTION_MODE0 | SPI_CONFIG_OPTION_CS_DBUS4 | SPI_CONFIG_OPTION_CS_ACTIVELOW;
	channelConf.Pin = 0x00000000;/*FinalVal-FinalDir-InitVal-InitDir (for dir 0=in, 1=out)*/
    
    //count number of mismatches
    int mismatches_count = 0;

	/* init library */
#ifdef _MSC_VER
	Init_libMPSSE();
#endif
	status = SPI_GetNumChannels(&channels);
	APP_CHECK_STATUS(status);
	printf("Number of available SPI channels = %d\n",(int)channels);
    
    int correct_channel = -1;
    char * correct_serial_number = "VER00001A";
    char * correct_serial_number2 = "VER00001B";

	if(channels>0)
	{
		for(i=0;i<channels;i++)
		{
			status = SPI_GetChannelInfo(i,&devList);
			APP_CHECK_STATUS(status);
			//printf("Information on channel number %d:\n",i);
			/* print the dev info */
			//printf("		Flags=0x%x\n",devList.Flags);
			//printf("		Type=0x%x\n",devList.Type);
			//printf("		ID=0x%x\n",devList.ID);
			//printf("		LocId=0x%x\n",devList.LocId);
			//printf("		SerialNumber=%s\n",devList.SerialNumber);
			//printf("		Description=%s\n",devList.Description);
			//printf("		ftHandle=0x%x\n",(unsigned int)devList.ftHandle);/*is 0 unless open*/
            
            if(strcmp(devList.SerialNumber, correct_serial_number) == 0) 
            {
                correct_channel = i;
                printf("Choosing the following channel (Channel #%d):\n", correct_channel);
                printf("Information on channel number %d:\n",i);
                // /* print the dev info */
                printf("		Flags=0x%x\n",devList.Flags);
                printf("		Type=0x%x\n",devList.Type);
                printf("		ID=0x%x\n",devList.ID);
                printf("		LocId=0x%x\n",devList.LocId);
                printf("		SerialNumber=%s\n",devList.SerialNumber);
                printf("		Description=%s\n",devList.Description);
                printf("		ftHandle=0x%x\n",(unsigned int)devList.ftHandle);/*is 0 unless open*/
                break;
            }
            
            if(strcmp(devList.SerialNumber, correct_serial_number2) == 0) 
            {
                correct_channel = i;
                printf("Choosing the following channel (Channel #%d):\n", correct_channel);
                printf("Information on channel number %d:\n",i);
                // /* print the dev info */
                printf("		Flags=0x%x\n",devList.Flags);
                printf("		Type=0x%x\n",devList.Type);
                printf("		ID=0x%x\n",devList.ID);
                printf("		LocId=0x%x\n",devList.LocId);
                printf("		SerialNumber=%s\n",devList.SerialNumber);
                printf("		Description=%s\n",devList.Description);
                printf("		ftHandle=0x%x\n",(unsigned int)devList.ftHandle);/*is 0 unless open*/
                break;
            }
		}
        
        if(correct_channel == -1)
        {
            fprintf(stderr,"can't find programming cable HW-USBN-2B\n");
            return 1;
        }

		/* Open the first available channel */
		//status = SPI_OpenChannel(CHANNEL_TO_OPEN,&ftHandle);
		status = SPI_OpenChannel(correct_channel,&ftHandle);
		APP_CHECK_STATUS(status);
		printf("\nhandle=0x%x status=0x%x\n",(unsigned int)ftHandle,status);
		status = SPI_InitChannel(ftHandle,&channelConf);
		APP_CHECK_STATUS(status);

#if USE_WRITEREAD	
		{
            int ERROR_CODE = 0;
            
            FILE *fp;
            char * fileName = argv[1];
            
            char lineBuffer[1024];
            int lineBufferSize = 1024;
            fp = fopen(fileName, "r");
            
            if(fp == NULL) 
            {
                fprintf(stderr, "Failed to open file %s", fileName);
                //exit(1);
                return 1;
            }
            char *blockCommentStart = "/*";
            char *blockCommentEnd = "*/";
            char *comment = "//";
            
			uint8 k,l;
            int len;
			uint8 inBuffer[100];
			//uint8 outBuffer[]={0x81,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,30,31,32,33,34,35,36,37,38,39};
            uint8 outBuffer[1024];
			uint32 sizeToTransfer,sizeTransferred;
            //uint addr;
            char* ptr;
            uint8 write;
            
            int r_w_pos = 2;
            int start_pos_r = 4;
            
            
            k = 0;
            
            FILE *output_fp = fopen(outputFile, "w");

			while(!feof(fp))
			{
                if(fgets(lineBuffer, lineBufferSize, fp) != NULL)
                {
                    if(hasPrefix(lineBuffer, comment) == 0)
                        continue;
                    
                    if(hasPrefix(lineBuffer, blockCommentStart) == 0)
                    {
                        while(!feof(fp))
                        {
                            if(fgets(lineBuffer, lineBufferSize, fp) != NULL) 
                            {
                                if(hasSuffix(lineBuffer, blockCommentEnd) == 0)
                                {
                                    break;
                                }
                                else
                                    continue;
                            }
                        }                    
                    }
                    
                    strtok(lineBuffer, "\n"); //ignore new lines 
                    if(hasSuffix(lineBuffer, blockCommentEnd) != 0) 
                    {
                        len = str_2_hex(lineBuffer, outBuffer);
                        print_data(outBuffer, len);
                        sizeToTransfer = len;
                        sizeTransferred = 0;  
                        
                        status = SPI_ReadWrite(ftHandle, inBuffer, outBuffer, sizeToTransfer, &sizeTransferred,
                                    SPI_TRANSFER_OPTIONS_SIZE_IN_BYTES|
                                    SPI_TRANSFER_OPTIONS_CHIPSELECT_ENABLE|
                                    SPI_TRANSFER_OPTIONS_CHIPSELECT_DISABLE);
                        

                        APP_CHECK_STATUS(status);
                        
                        int readCheck = 0;
                        
                        if(len > 3)
                        {
                            //readCheck = ((int) outBuffer[r_w_pos] == 9) ? 1 : 0;
                            readCheck = ((int) outBuffer[r_w_pos] % 2 == 1) ? 1 : 0;
                        }
                        
                        if(readCheck) 
                        {
                            for(l = 0; l < sizeToTransfer; l++)
                            {
                                if(l == 0)
                                    fprintf(output_fp, "00 ");
                                else if (l == 1)
                                    fprintf(output_fp, "00 ");
                                else
                                    fprintf(output_fp, "%02X ", outBuffer[l]);
                            }
                            uint8 z;
                            int check;
                            for(z = start_pos_r; z < sizeToTransfer; z++) 
                            {
                                //check = (outBuffer[z] == inBuffer[z]) ? 1 : 0;
                                check = (outBuffer[z] == inBuffer[z]) ? 1 : 0;
                                if(!check)
                                {
                                    fprintf(output_fp, "*****[MISMATCH]Data Read Back: %02X, Found: %02X*****", inBuffer[z], outBuffer[z]);
                                    /*
                                    printf("Found Mismatch:");
                                    print_data(outBuffer, len);
                                    printf("Read Expected\n");
                                    
                                    uint8 ttl;
                                    for(ttl = start_pos_r; ttl < sizeToTransfer; ttl++) 
                                    {
                                        printf(" %02X     %02X\n", inBuffer[ttl], outBuffer[ttl]);
                                    }
                                    */
                                    mismatches_count++;
                                    break;
                                }
                                
                            }
                            
                            if(check)
                            {
                                fprintf(output_fp, "[MATCH]Data Read Back: ");
                                uint8 e;
                                for(e = start_pos_r; e < sizeToTransfer; e++) 
                                {
                                    fprintf(output_fp, "%02X ", inBuffer[e]);
                                }
                            }
                            
                            fprintf(output_fp, "\n");
                            
                            //check if mismatch
                            /*
                            int check = (outBuffer[sizeToTransfer - 1] == inBuffer[sizeToTransfer - 1]) ? 1 : 0;
                            if(check)
                                fprintf(output_fp, "*****[MATCH]Data Read Back: %02X*****", inBuffer[sizeToTransfer - 1]);
                            else
                                fprintf(output_fp, "***[MISMATCH]Data Read Back: %02X****", inBuffer[sizeToTransfer - 1]);
                            
                            fprintf(output_fp, "\n");
                            */
                        }
                        else
                        {
                            for(l = 0; l < sizeToTransfer; l++)
                                fprintf(output_fp, "00 ");
                            fprintf(output_fp, "\n");
                        }
                        
                        //printf("status=0x%x data = %#08x sizeTransferred=%u\n", status, inBuffer, sizeTransferred);
                        // printf("status=0x%x, sizeTransferred=%u\n", status, sizeTransferred);
                        //for(l=0;l<sizeToTransfer;l++)
                        //    printf("0x%x\n",(unsigned)inBuffer[l]);
                        //printf("===========\n");
                        //for(l=0;l<sizeToTransfer;l++)
                        //    printf("0x%x\n",(unsigned)outBuffer[l]);
                        // printf("\n");
                    }
                    k++;
                    
                }
			}
            fclose(fp);
            
            //print pass or fail
            if(mismatches_count > 0)
                fprintf(output_fp, "Found %d mismatches\nFailed", mismatches_count);
            else
                fprintf(output_fp, "Passed");
            
            fclose(output_fp);
		}		
#else // USE_WRITEREAD
		for(address=START_ADDRESS_EEPROM;address<END_ADDRESS_EEPROM;address++)
		{
			printf("writing address = %02d data = %d\n",address,address+DATA_OFFSET);
			write_byte(SPI_SLAVE_0, address,(uint16)address+DATA_OFFSET);
		}

 		for(address=START_ADDRESS_EEPROM;address<END_ADDRESS_EEPROM;address++)
		{
			read_byte(SPI_SLAVE_0, address,&data);
			printf("reading address = %02d data = %d\n",address,data);
		} 
#endif // USE_WRITEREAD


		status = SPI_CloseChannel(ftHandle);
	}
#ifdef _MSC_VER
	Cleanup_libMPSSE();
#endif
    if(mismatches_count > 0)
        return 1;
    else
        return 0;
}

