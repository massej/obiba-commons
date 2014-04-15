package org.obiba.git.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.obiba.git.CommitInfo;
import org.obiba.git.GitException;
import org.obiba.git.NoSuchGitRepositoryException;
import org.obiba.git.TagInfo;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class GitCommandsTest {

  private final GitCommandHandler handler = new GitCommandHandler();

  @BeforeClass
  public static void init() {
    SecurityUtils.setSecurityManager(new DefaultSecurityManager());
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void test_create_read_files() throws Exception {

    File repo = getRepoPath();

    try(InputStream input1 = new FileInputStream(createFile("This is root file"));
        InputStream input2 = new FileInputStream(createFile("This is a file in dir"))) {
      handler.execute(new AddFilesCommand.Builder(repo, "Initial commit") //
          .addFile("root.txt", input1) //
          .addFile("dir/file.txt", input2) //
          .build());
    }

    assertThat(readFile(repo, "root.txt")).isEqualTo("This is root file");
    assertThat(readFile(repo, "dir/file.txt")).isEqualTo("This is a file in dir");

    Iterable<CommitInfo> commitInfos = handler.execute(new LogsCommand.Builder(repo).build());
    assertThat(commitInfos).hasSize(1);
    CommitInfo commitInfo = Iterables.getFirst(commitInfos, null);
    assertThat(commitInfo).isNotNull();
    assertThat(commitInfo.getAuthorName()).isEqualTo(AbstractGitWriteCommand.DEFAULT_AUTHOR_NAME);
    assertThat(commitInfo.getAuthorEmail()).isEqualTo(AbstractGitWriteCommand.DEFAULT_AUTHOR_EMAIL);
    assertThat(commitInfo.isHead()).isTrue();
    assertThat(commitInfo.isCurrent()).isTrue();
    assertThat(commitInfo.getComment()).isEqualTo("Initial commit");

    try {
      handler.execute(new ReadFileCommand.Builder(repo, "none").build());
      fail("Should throw GitException");
    } catch(GitException e) {
      assertThat(e).hasRootCauseExactlyInstanceOf(FileNotFoundException.class);
    }
  }

  @Test(expected = NoSuchGitRepositoryException.class)
  public void test_read_files_from_invalid_repo() throws Exception {
    handler.execute(new ReadFileCommand.Builder(getRepoPath(), "file.txt").build());
  }

  @Test
  public void test_update_read_files() throws Exception {

    File repo = getRepoPath();

    try(InputStream input = new FileInputStream(createFile("Version 1"))) {
      handler.execute(new AddFilesCommand.Builder(repo, "First commit").addFile("root.txt", input).build());
    }
    try(InputStream input = new FileInputStream(createFile("Version 2"))) {
      handler.execute(new AddFilesCommand.Builder(repo, "Second commit").addFile("root.txt", input).build());
    }

    Iterable<CommitInfo> commitInfos = handler.execute(new LogsCommand.Builder(repo).build());
    assertThat(commitInfos).hasSize(2);

    assertThat(readFile(repo, "root.txt")).isEqualTo("Version 2");
  }

  @Test
  @SuppressWarnings("ConstantConditions")
  public void test_tags() throws Exception {

    File repo = getRepoPath();

    try(InputStream input = new FileInputStream(createFile("Version 1"))) {
      handler.execute(new AddFilesCommand.Builder(repo, "First commit").addFile("root.txt", input).build());
    }

    assertThat(handler.execute(new TagListCommand.Builder(repo).build())).isEmpty();

    handler.execute(new TagCommand.Builder(repo, "Test first tag", "1.0").build());

    Iterable<TagInfo> tagInfos = handler.execute(new TagListCommand.Builder(repo).build());
    assertThat(tagInfos).hasSize(1);
    TagInfo tagInfo = Iterables.getFirst(tagInfos, null);
    assertThat(tagInfo).isNotNull();
    assertThat(tagInfo.getName()).isEqualTo("1.0");
    assertThat(tagInfo.getCommitId()).isNotEmpty();

    assertThat(readFileFromTag(repo, "root.txt", "1.0")).isEqualTo("Version 1");
    assertThat(readFileFromCommit(repo, "root.txt", tagInfo.getCommitId())).isEqualTo("Version 1");
  }

  private File getRepoPath() throws IOException {
    File repo = File.createTempFile("obiba", "git");
    // delete it so we create a new repo
    if(!repo.delete()) {
      throw new IllegalStateException("Cannot delete git repo " + repo.getAbsolutePath());
    }
    return repo;
  }

  private String readFile(File repo, String path) throws Exception {
    return readFile(repo, path, null, null);
  }

  private String readFileFromTag(File repo, String path, String tag) throws Exception {
    return readFile(repo, path, null, tag);
  }

  private String readFileFromCommit(File repo, String path, String commitId) throws Exception {
    return readFile(repo, path, commitId, null);
  }

  private String readFile(File repo, String path, String commitId, String tag) throws Exception {
    InputStream inputStream = handler
        .execute(new ReadFileCommand.Builder(repo, path).commitId(commitId).tag(tag).build());
    return CharStreams.toString(new InputStreamReader(inputStream, Charsets.UTF_8));
  }

  private File createFile(CharSequence content) throws IOException {
    File file = File.createTempFile("obiba", "git");
    file.deleteOnExit();
    Files.write(content, file, Charsets.UTF_8);
    return file;
  }
}
