import http from '@/utils/httpRequest.js'
export function policy(fileName) {
   return  new Promise((resolve,reject)=>{
        http({
            url: http.adornUrl("/thirdparty/cos/policy"),
            method: "get",
            params: http.adornParams({fileName: fileName})
        }).then(({ data }) => {
            resolve(data);
        })
    });
}

export function getAccessUrl(key) {
  return new Promise((resolve, reject) => {
    http({
      url: http.adornUrl("/thirdparty/cos/access"),
      method: "get",
      params: http.adornParams({ key })
    }).then(({ data }) => {
      resolve(data.data)
    })
  })
}

export function getDeleteUrl(key) {
  return new Promise((resolve, reject) => {
    http({
      url: http.adornUrl("/thirdparty/cos/delete"),
      method: "get",
      params: http.adornParams({ key })
    }).then(({ data }) => {
      resolve(data.data)
    })
  })
}
