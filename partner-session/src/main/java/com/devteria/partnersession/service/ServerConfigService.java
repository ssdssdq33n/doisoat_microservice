package com.devteria.partnersession.service;

import static com.devteria.partnersession.Constants.Constant.FILES_COUNT.MAXFILES;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;

import com.devteria.partnersession.dto.response.SalesPartnerResponse;
import com.devteria.partnersession.dto.response.SupplyPartnerResponse;
import com.devteria.partnersession.mapper.SalesPartnerMapper;
import com.devteria.partnersession.mapper.SupplyPartnerMapper;
import com.devteria.partnersession.model.ServerConfig;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// @Service
public class ServerConfigService {

    SalesPartnerService salesPartnerService;

    SupplyPartnerService supplyPartnerService;

    SupplyPartnerMapper supplyPartnerMapper;

    SalesPartnerMapper salesPartnerMapper;

    @Value("${proton.report.save.path}")
    @NonFinal
    private String fileSavePath;

    //    @Scheduled(fixedRate = 6000)
    public void downloadNewestFileFromSFTP() {
        List<SalesPartnerResponse> salesPartners = salesPartnerService.getAllSalesPartner();
        List<SupplyPartnerResponse> supplyPartners = supplyPartnerService.getAllSupplyPartner();

        List<ServerConfig> serverConfigs = new ArrayList<>(supplyPartnerMapper.toServerConfig(supplyPartners));
        serverConfigs.addAll(salesPartnerMapper.toServerConfig(salesPartners));
        Session session = null;
        ChannelSftp channelSftp = null;
        for (ServerConfig config : serverConfigs) {
            try {
                // Tạo kết nối đến SFTP server bằng jsch
                JSch jsch = new JSch();
                session = jsch.getSession(config.getUsername(), config.getIp(), Integer.parseInt(config.getPort()));
                session.setPassword(config.getPassword());

                Properties properties = new Properties();
                properties.put("StrictHostKeyChecking", "no");
                session.setConfig(properties);

                session.connect();

                channelSftp = (ChannelSftp) session.openChannel("sftp");
                channelSftp.connect();
                // Tạo kết nối xong

                // Liệt kê các file trong thư mục, chọn file mới nhất
                @SuppressWarnings("unchecked")
                Vector<ChannelSftp.LsEntry> files = channelSftp.ls(config.getDownloadPath());
                List<ChannelSftp.LsEntry> newestFiles = new ArrayList<>();

                newestFiles = findNewestFilesFromList(files, MAXFILES);

                // Download file trên sFTP server(remoteFilePath) về local(localFilePath) với tên file tương ứng
                if (config.getSftpServerName().equalsIgnoreCase("Iris")
                        && LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY) {
                    // Xử lý 3 file mới nhất
                    for (ChannelSftp.LsEntry newestFile : newestFiles) {
                        // Xử lý file
                        downloadAndSaveFile(channelSftp, config, newestFile, fileSavePath);
                    }
                } else {
                    downloadAndSaveFile(channelSftp, config, newestFiles.getLast(), fileSavePath);
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // Đóng các kết nối channelSftp - session
                if (channelSftp != null && channelSftp.isConnected()) {
                    channelSftp.disconnect();
                }
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            }
        }
    }

    private void downloadAndSaveFile(
            ChannelSftp channelSftp, ServerConfig config, ChannelSftp.LsEntry newestFile, String fileSavePath)
            throws IOException {
        LocalDate currentDate = LocalDate.now().minusDays(1);
        String day = String.format("%02d", currentDate.getDayOfMonth());
        String month = String.format("%02d", currentDate.getMonthValue());
        String year = String.valueOf(currentDate.getYear());
        String remoteFilePath = config.getDownloadPath() + "/" + newestFile.getFilename();
        String localFilePath;
        if ("sacombank".equalsIgnoreCase(config.getSftpServerName())) {
            localFilePath = fileSavePath + "\\" + year + "\\" + config.getSftpServerName() + "\\DS-T" + month;
        } else {
            localFilePath = fileSavePath + "\\" + year + "\\" + config.getSftpServerName() + "\\DS-T" + month + "\\"
                    + day + "-" + month;
        }

        Files.createDirectories(Path.of(localFilePath));
        localFilePath = localFilePath + "\\" + newestFile.getFilename();

        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            System.out.println("File already exists. no Overwriting: " + localFilePath);
            return;
        }

        try (InputStream inputStream = channelSftp.get(remoteFilePath);
                FileOutputStream fileOutputStream = new FileOutputStream(localFilePath)) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("File downloaded successfully to " + localFilePath);
        } catch (Exception e) {
            // Handle any exceptions here
            e.printStackTrace();
        }
    }

    private static List<ChannelSftp.LsEntry> findNewestFilesFromList(Vector<ChannelSftp.LsEntry> files, int maxFiles) {
        List<ChannelSftp.LsEntry> newestFiles = new ArrayList<>();

        for (ChannelSftp.LsEntry file : files) {
            if (!file.getAttrs().isDir()) {
                if (newestFiles.size() < maxFiles
                        || file.getAttrs().getMTime()
                                > newestFiles.get(0).getAttrs().getMTime()) {
                    newestFiles.add(file);
                    newestFiles.sort(Comparator.comparingLong(e -> e.getAttrs().getMTime()));
                    if (newestFiles.size() > maxFiles) {
                        newestFiles.remove(0);
                    }
                }
            }
        }
        return newestFiles;
    }
}
