package org.pharmgkb.pharmcat.definition;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import javax.annotation.Nonnull;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.base.Preconditions;
import com.google.gdata.util.ServiceException;
import org.pharmgkb.common.io.google.GoogleApiHelper;
import org.pharmgkb.common.io.google.GoogleSheetsHelper;


/**
 * This is a helper class for working with the Google Drive and Google Sheets API.
 * We use Drive to find the files and Sheets to export to TSV.
 *
 * @author Mark Woon
 */
public class SheetsHelper implements AutoCloseable {
  private static final String sf_serviceName = "PharmCAT";
  private GoogleApiHelper m_googleApiHelper;
  private GoogleSheetsHelper m_spreadsheetHelper;
  private Drive m_drive;


  public SheetsHelper(@Nonnull String user, @Nonnull String key) throws Exception {

    Preconditions.checkNotNull(user);
    Preconditions.checkNotNull(key);
    m_googleApiHelper = new GoogleApiHelper(user, key,
        GoogleSheetsHelper.SHEETS_SCOPE,
        DriveScopes.DRIVE_READONLY);
    m_spreadsheetHelper = new GoogleSheetsHelper(m_googleApiHelper, sf_serviceName);
    m_drive = new Drive.Builder(m_googleApiHelper.getHttpTransport(), m_googleApiHelper.getJsonFactory(),
        m_googleApiHelper.getCredential())
        .setApplicationName(sf_serviceName)
        .build();
  }


  @Override
  public void close() {
    m_googleApiHelper.close();
  }


  /**
   * Downloads all allele definitions as TSV files.
   * <p>
   * This is the main entry point.
   */
  public void downloadAlleleDefinitions(@Nonnull Path outputDir) throws IOException, ServiceException {

    Preconditions.checkNotNull(outputDir);

    String folderId = findAlleleDefinitionsFolder().getId();
    List<File> files = getSheetsInDirectory(folderId);
    downloadAsTsv(files, outputDir);
  }


  public void downloadAsTsv(List<File> files, Path outputDir) throws IOException, ServiceException {

    for (File file : files) {
      m_spreadsheetHelper.exportToTsv(file.getId(), outputDir.resolve(file.getName().replaceAll("\\s+", "_") + ".tsv"));
    }
  }



  public List<File> getSheetsInDirectory(String folderId) throws IOException {

    Drive.Files.List request = m_drive.files().list();
    // get directory
    FileList files = request.setQ("'" + folderId + "' in parents and mimeType='application/vnd.google-apps.spreadsheet' " +
        "and trashed=false")
        .execute();
    return files.getFiles();
  }


  public @Nonnull File findAlleleDefinitionsFolder() throws IOException {

    Drive.Files.List request = m_drive.files().list();
    // get directory
    FileList files = request.setQ("name='Allele Definitions' and mimeType='application/vnd.google-apps.folder' " +
        "and sharedWithMe and trashed=false")
        .execute();
    if (files.getFiles().size() == 0) {
      throw new IOException("Cannot find 'Allele Definitions' folder on Drive");
    } else if (files.getFiles().size() > 1) {
      throw new IOException("Found " + files.getFiles().size() + " 'Allele Definitions' folder on Drive!");
    }
    return files.getFiles().get(0);
  }
}
