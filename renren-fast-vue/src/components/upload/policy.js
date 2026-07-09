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
