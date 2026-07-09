<template> 
  <div>
    <el-upload
      :http-request="cosUpload"
      list-type="picture"
      :multiple="false" :show-file-list="showFileList"
      :file-list="fileList"
      :on-remove="handleRemove"
      :on-preview="handlePreview">
      <el-button size="small" type="primary">点击上传</el-button>
      <div slot="tip" class="el-upload__tip">只能上传jpg/png文件，且不超过10MB</div>
    </el-upload>
    <el-dialog :visible.sync="dialogVisible">
      <img width="100%" :src="previewUrl" alt="">
    </el-dialog>
  </div>
</template>
<script>
   import {policy} from './policy'

  export default {
    name: 'singleUpload',
    props: {
      value: String
    },
    computed: {
      imageName() {
        if (this.value != null && this.value !== '') {
          return this.value.substr(this.value.lastIndexOf("/") + 1);
        } else {
          return null;
        }
      },
      fileList() {
        if (this.previewUrl) {
          return [{name: this.imageName, url: this.previewUrl}]
        }
        return []
      },
      showFileList: {
        get: function () {
          return this.value !== null && this.value !== ''&& this.value!==undefined;
        },
        set: function (newValue) {
        }
      }
    },
    data() {
      return {
        dialogVisible: false,
        previewUrl: ''
      };
    },
    watch: {
      value: {
        immediate: true,
        handler(val) {
          if (val) {
            this.fetchPreviewUrl(val)
          } else {
            this.previewUrl = ''
          }
        }
      }
    },
    methods: {
      emitInput(val) {
        this.$emit('input', val)
      },
      fetchPreviewUrl(key) {
        this.$http({
          url: this.$http.adornUrl("/thirdparty/cos/access"),
          method: "get",
          params: this.$http.adornParams({key: key})
        }).then(({ data }) => {
          if (data && data.code === 0) {
            this.previewUrl = data.data
          }
        })
      },
      handleRemove(file, fileList) {
        this.emitInput('');
      },
      handlePreview(file) {
        this.dialogVisible = true;
      },
      cosUpload(uploadFile) {
        policy(uploadFile.file.name).then(resp => {
          const uploadUrl = resp.data;
          const key = resp.key;
          fetch(uploadUrl, {
            method: 'PUT',
            headers: { 'Content-Type': uploadFile.file.type || 'application/octet-stream' },
            body: uploadFile.file
          }).then(() => {
            this.fetchPreviewUrl(key)
            this.emitInput(key)
          })
        })
      }
    }
  }
</script>
<style>

</style>

