package net.valraiso.sbt.minimize

import sbt._
import sbt.Keys._
import com.typesafe.sbt.web.{incremental, SbtWeb, PathMapping}
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.sbt.web.incremental._
import sbt.Task
import spray.json.{JsArray, JsBoolean, JsString, JsObject}

object Import {

  val minimize = TaskKey[Pipeline.Stage]("minimize", "Minimize")

  object MinimizeKeys {
    val buildDir = SettingKey[File]("minimize-build-dir", "Where minimize will read from.")
    val emptyAttr = SettingKey[Boolean]("minimize-emptyAttr", "do not remove empty attributes")
    val cdata = SettingKey[Boolean]("minimize-cdata", "do not strip CDATA from scripts")
    val comments = SettingKey[Boolean]("minimize-comments", "do not remove comments")
    val conditionals = SettingKey[Boolean]("minimize-conditionals", "do not remove conditional internet explorer comments")
    val quotes = SettingKey[Boolean]("minimize-quotes", "do not remove arbitrary quotes")
  }

}

object SbtMinimize extends AutoPlugin {

  override def requires = SbtJsTask

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import autoImport._
  import MinimizeKeys._

  override def projectSettings = Seq(
    buildDir := (resourceManaged in minimize).value / "build",
    excludeFilter in minimize := HiddenFileFilter,
    includeFilter in minimize := GlobFilter("*.html"),
    resourceManaged in minimize := webTarget.value / minimize.key.label,
    emptyAttr := false,
    cdata := true,
    comments := false,
    conditionals := false,
    quotes := false,
    minimize := runMinimizeHtml.dependsOn(WebKeys.nodeModules in Plugin).value
  )

  private def runMinimizeHtml: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    mappings =>

      val include = (includeFilter in minimize).value
      val exclude = (excludeFilter in minimize).value
      val minimizeMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))

      SbtWeb.syncMappings(
        streams.value.cacheDirectory,
        minimizeMappings,
        buildDir.value
      )

      val buildMappings = minimizeMappings.map(o => buildDir.value / o._2)

      val cacheDirectory = streams.value.cacheDirectory / minimize.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        inputFiles =>
          streams.value.log.info("Minimize")

          val sourceFileMappings = JsArray(inputFiles.filter(_.isFile).map { f =>
            val relativePath = IO.relativize(buildDir.value, f).get
            JsArray(JsString(f.getPath), JsString(relativePath))
          }.toList).toString()

          val targetPath = buildDir.value.getPath
          val jsOptions = JsObject(
              "emptyAttr" -> JsBoolean(emptyAttr.value),
              "cdata" -> JsBoolean(cdata.value),
              "comments" -> JsBoolean(comments.value),
              "conditionals" -> JsBoolean(conditionals.value),
              "quotes" -> JsBoolean(quotes.value)
          ).toString()

          val shellFile = SbtWeb.copyResourceTo(
            (resourceManaged in minimize).value,
            getClass.getClassLoader.getResource("minimize-shell.js"),
            streams.value.cacheDirectory
          )

          SbtJsTask.executeJs(
            state.value,
            (engineType in minimize).value,
            (command in minimize).value,
            (nodeModuleDirectories in Assets).value.map(_.getPath),            
            shellFile,
            Seq(sourceFileMappings, targetPath, jsOptions),
            (timeoutPerSource in minimize).value * minimizeMappings.size
          )

          buildDir.value.***.get.filter(!_.isDirectory).toSet
      }

      val minimizedMappings = runUpdate(buildMappings.toSet).pair(relativeTo(buildDir.value))
      (mappings.toSet -- minimizeMappings ++ minimizedMappings).toSeq
  }

}