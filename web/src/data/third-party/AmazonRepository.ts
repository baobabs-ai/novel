import ky, { Options } from 'ky';

export const createAmazonRepository = () => {
  const getHtml = async (url: string, options?: Options) => {
    const response = await ky.get(url, {
      prefixUrl: 'https://www.amazon.co.jp',
      redirect: 'manual',
      credentials: 'include',
      retry: 0,
      ...options,
    });

    if (response.status === 404) {
      throw Error('Novel does not exist, please delete cookies and try again with a Japanese IP');
    } else if (response.status === 0) {
      throw Error('Age limit triggered, please use the plugin as instructed');
    } else if (!response.ok) {
      throw Error(`Unknown error, ${response.status}`);
    }
    const html = await response.text();
    const parser = new DOMParser();
    const doc = parser.parseFromString(html, 'text/html');
    return doc;
  };

  const getProduct = (asin: string) => getHtml(`dp/${asin}`);

  const getSerial = (asin: string, total: string) =>
    getHtml('kindle-dbs/productPage/ajax/seriesAsinList', {
      searchParams: {
        asin,
        pageNumber: 1,
        pageSize: total,
      },
    });

  const search = (query: string) =>
    getHtml('s', {
      searchParams: {
        k: query,
        i: 'stripbooks',
      },
    });

  return {
    getProduct,
    getSerial,
    search,
  };
};
