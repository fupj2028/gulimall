<template>
  <div>
    <el-upload
      :http-request="uploadFile"
      list-type="picture-card"
      :file-list="fileList"
      :on-remove="handleRemove"
      :on-success="handleUploadSuccess"
      :on-preview="handlePreview"
      :limit="maxCount"
      :on-exceed="handleExceed"
    >
      <i class="el-icon-plus"></i>
    </el-upload>
    <el-dialog :visible.sync="dialogVisible">
      <img width="100%" :src="dialogImageUrl" alt />
    </el-dialog>
  </div>
</template>
<script>
import { policy, getAccessUrl, getDeleteUrl } from "./policy";

export default {
  name: "multiUpload",
  props: {
    value: Array,
    maxCount: {
      type: Number,
      default: 30
    }
  },
  data() {
    return {
      fileList: [],
      dialogVisible: false,
      dialogImageUrl: null,
      _init: false
    };
  },
  watch: {
    value: {
      immediate: true,
      handler(val) {
        if (this._init) {
          this._init = false;
          return;
        }
        this.loadKeys(val);
      }
    }
  },
  methods: {
    loadKeys(keys) {
      if (!keys || keys.length === 0) {
        this.fileList = [];
        return;
      }
      this.fileList = keys.map(key => ({ name: key, url: '', key }));
      keys.forEach(key => {
        getAccessUrl(key).then(url => {
          let item = this.fileList.find(f => f.key === key);
          if (item) item.url = url;
        });
      });
    },
    emitInput(fileList) {
      this._init = true;
      this.$emit("input", fileList.map(f => f.key));
    },
    async uploadFile(fileObj) {
      try {
        let res = await policy(fileObj.file.name);
        let presignedUrl = res.data;
        let key = res.key;
        let uploadRes = await fetch(presignedUrl, { method: 'PUT', body: fileObj.file });
        if (uploadRes.ok) {
          let blobUrl = URL.createObjectURL(fileObj.file);
          fileObj.onSuccess({ key, url: blobUrl });
        } else {
          fileObj.onError({ message: 'upload failed' });
        }
      } catch (err) {
        fileObj.onError(err);
      }
    },
    handleUploadSuccess(res, file) {
      this.fileList.push({
        name: res.key,
        url: res.url,
        key: res.key
      });
      this.emitInput(this.fileList);
    },
    async handleRemove(file, fileList) {
      this.fileList = fileList;
      try {
        let deleteUrl = await getDeleteUrl(file.key);
        await fetch(deleteUrl, { method: 'DELETE' });
      } catch (err) {
        console.error('COS文件删除失败', err);
      }
      this.emitInput(this.fileList);
    },
    async handlePreview(file) {
      try {
        let url = await getAccessUrl(file.key);
        this.dialogImageUrl = url;
        this.dialogVisible = true;
      } catch (err) {
        this.$message({ message: "预览失败", type: "error" });
      }
    },
    handleExceed(files, fileList) {
      this.$message({
        message: "最多只能上传" + this.maxCount + "张图片",
        type: "warning",
        duration: 1000
      });
    }
  }
};
</script>
<style>
</style>
